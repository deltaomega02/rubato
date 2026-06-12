<?php
header("Content-Type: application/json; charset=UTF-8");
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

class TravelPlanner {
    private $conn;
    private $apiKey;
    private $sessionId;
    private $userEmail;
    private $placeApiKey;
    

    // Place API와 Geocoding을 통해 장소 정보 가져오기 메서드 추가
    private function getPlaceDetails($placeName) {
        // Place API로 장소 검색
        $placeUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json"
            . "?query=" . urlencode($placeName)
            . "&key=YOUR_GOOGLE_API_KEY"
            . "&language=ko";

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $placeUrl);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $placeResponse = curl_exec($ch);
        curl_close($ch);

        $placeResult = json_decode($placeResponse, true);

        if (!isset($placeResult['results'][0])) {
            return null;
        }

        $place = $placeResult['results'][0];

        // Geocoding으로 주소의 정확한 위도/경도 가져오기
        $geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json"
            . "?address=" . urlencode($place['formatted_address'])
            . "&key=YOUR_GOOGLE_API_KEY"
            . "&language=ko";

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $geocodeUrl);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $geocodeResponse = curl_exec($ch);
        curl_close($ch);

        $geocodeResult = json_decode($geocodeResponse, true);

        if (!isset($geocodeResult['results'][0]['geometry']['location'])) {
            return null;
        }

        return [
            'name' => $place['name'],
            'address' => $place['formatted_address'],
            'latitude' => $geocodeResult['results'][0]['geometry']['location']['lat'],
            'longitude' => $geocodeResult['results'][0]['geometry']['location']['lng']
        ];
    }

    public function __construct($conn, $apiKey, $sessionId, $userEmail, $placeApiKey) {
        $this->conn = $conn;
        $this->apiKey = $apiKey;
        $this->sessionId = $sessionId;
        $this->userEmail = $userEmail;
        $this->placeApiKey = $placeApiKey;
    }
    
    // 채팅 기록 가져오기
    private function getChatHistory() {
        error_log("[ChatHistory] Fetching chat history for session: " . $this->sessionId);

        $stmt = $this->conn->prepare("
            SELECT who, user_data, gpt_data, created_at
            FROM chat_history 
            WHERE session_id = :session_id 
            ORDER BY created_at ASC
        ");
        $stmt->execute(['session_id' => $this->sessionId]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // 태그 정보 가져오기
    private function getTags() {
        $stmt = $this->conn->prepare("SELECT * FROM Tag");
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    // 장소 정보 가져오기
    private function getPlaces() {
        $stmt = $this->conn->prepare("
            SELECT place_name, address, latitude, longitude, tags
            FROM Place
            WHERE is_active = 1
        ");
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // GPT 시스템 메시지 생성
    private function createSystemMessage($chatHistory) {
        $tags = $this->getTags();
        $tagList = implode(", ", array_column($tags, 'tag_name'));

        global $promptData;

        $selectedDates = isset($promptData['selectedDates']) ? implode(", ", $promptData['selectedDates']) : "선택된 날짜가 없습니다.";
        $selectedLocations = isset($promptData['selectedLocations']) ? implode(", ", $promptData['selectedLocations']) : "선택된 지역이 없습니다.";
    

        $placeList = "";
        if (isset($promptData['allPlaceNames'])) {
            for ($i = 0; $i < count($promptData['allPlaceNames']); $i++) {
                $placeList .= sprintf(
                    "- %s (주소: %s, 위도: %s, 경도: %s)\n",
                    $promptData['allPlaceNames'][$i],
                    $promptData['allPlaceAddresses'][$i],
                    $promptData['allLatitudes'][$i],
                    $promptData['allLongitudes'][$i]
                );
            }
        }

        $systemMessage = <<<EOT
당신은 친근하고 전문적인 여행 플래너입니다. 사용자와의 자연스러운 대화를 통해 맞춤형 여행 일정을 계획해야 합니다.

현재 사용자가 선택한 정보는 다음과 같습니다:
- 선택된 날짜: {$selectedDates}
- 선택된 지역: {$selectedLocations}

1. 수집해야 할 정보:
   - 여행 스타일 (타이트한 일정 vs 여유로운 일정)
   - 식사 선호도와 특별한 요구사항
   - 선호하는 관광지 유형 (가능한 태그: {$tagList})
   - 시간대별 선호 활동 (오전/오후/저녁/밤)
   - 특별한 요구사항이나 제약사항

2. 대화 가이드라인:
   - 직접적인 질문을 피하고 자연스러운 대화로 정보를 이끌어내세요
   - 한 번에 한 가지 주제만 다루세요
   - 이전 대화 내용을 참고하여 중복된 내용을 피하세요
   - 공감과 이해를 보여주는 실제 친구같은 대화를 하세요
   - 필요한 정보가 부족할 경우 자연스럽게 대화를 유도하세요

3. 사용 가능한 장소 목록:
{$placeList}

4. 응답 형식:
   대화 단계일 경우: 자연스러운 대화체로 응답

   일정 계획 단계일 경우 다음 JSON 형식으로만 응답:
   {
     "response": "일반적인 응답 메시지",
     "schedule": {
       "1": [{"name":"장소1","address":"주소1","latitude":"위도1","longitude":"경도1"}],
       "2": [{"name":"장소2","address":"주소2","latitude":"위도2","longitude":"경도2"}]
     }
   }

5. 주의사항:
   - 장소 선택시 우선적으로 제공된 장소 목록을 활용하세요
   - 필요한 장소가 목록에 없을 경우 place_api를 통해 검색하세요
   - 이동 시간과 체류 시간을 고려한 현실적인 일정을 계획하세요
   - 사용자의 선호도와 제약사항을 반영한 맞춤형 일정을 제공하세요
EOT;

        return $systemMessage;
    }
    
    // Place API 호출
    private function searchPlace($query) {
        $url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
             . "?query=" . urlencode($query)
             . "&key=" . $this->placeApiKey
             . "&language=ko";

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        $response = curl_exec($ch);
        curl_close($ch);

        $result = json_decode($response, true);

        if (isset($result['results'][0])) {
            $place = $result['results'][0];
            return [
                'name' => $place['name'],
                'address' => $place['formatted_address'],
                'latitude' => $place['geometry']['location']['lat'],
                'longitude' => $place['geometry']['location']['lng']
            ];
        }

        return null;
    }
    
    // 대화 단계 판단
    private function isReadyForSchedule($chatHistory) {
        $requiredInfo = [
            'travel_style' => false,
            'food_preference' => false,
            'attraction_type' => false,
            'time_preference' => false,
            'confirmation_requested' => false,
            'confirmed' => false
        ];

        $keywordPatterns = [
            'travel_style' => ['/타이트/u', '/여유/u', '/빡빡/u', '/느긋/u'],
            'food_preference' => ['/식사/u', '/맛집/u', '/음식/u', '/레스토랑/u', '/카페/u'],
            'time_preference' => ['/오전/u', '/오후/u', '/저녁/u', '/밤/u', '/새벽/u'],
            'confirmed' => ['/네/u', '/맞아요/u', '/좋아요/u', '/그렇게/u', '/동의/u', '/확인/u']
        ];

        $tags = $this->getTags();
        $tagPatterns = array_map(function($tag) {
            return '/' . preg_quote($tag['tag_name'], '/') . '/u';
        }, $tags);

        // 확인 요청 상태 확인
        foreach ($chatHistory as $chat) {
            if (isset($chat['gpt_data']) && strpos($chat['gpt_data'], '지금까지 제가 이해한 내용을 정리해드릴게요') !== false) {
                $requiredInfo['confirmation_requested'] = true;
            }
        }

        // 마지막 사용자 응답이 확인인지 체크
        if ($requiredInfo['confirmation_requested']) {
            $lastUserMessage = '';
            for ($i = count($chatHistory) - 1; $i >= 0; $i--) {
                if (isset($chatHistory[$i]['user_data'])) {
                    $lastUserMessage = $chatHistory[$i]['user_data'];
                    break;
                }
            }
            foreach ($keywordPatterns['confirmed'] as $pattern) {
                if (preg_match($pattern, $lastUserMessage)) {
                    $requiredInfo['confirmed'] = true;
                    break;
                }
            }
        }

        // 나머지 정보 체크
        foreach ($chatHistory as $chat) {
            $content = $chat['gpt_data'] ?? $chat['user_data'] ?? '';

            foreach ($keywordPatterns as $category => $patterns) {
                if ($category !== 'confirmed' && !$requiredInfo[$category]) {
                    foreach ($patterns as $pattern) {
                        if (preg_match($pattern, $content)) {
                            $requiredInfo[$category] = true;
                            break;
                        }
                    }
                }
            }

            if (!$requiredInfo['attraction_type']) {
                foreach ($tagPatterns as $pattern) {
                    if (preg_match($pattern, $content)) {
                        $requiredInfo['attraction_type'] = true;
                        break;
                    }
                }
            }
        }

        $minConversationTurns = 4;
        $conversationTurns = count($chatHistory) / 2;

        if ($requiredInfo['confirmed']) {
            return true;  // 최종 확인됨
        } else if ($allInfoCollected && !$requiredInfo['confirmation_requested']) {
            // 확인 요청이 필요한 상태임을 나타내는 특별한 메시지 추가
            $messages[] = ["role" => "system", "content" => "
                지금까지 수집된 정보를 자연스럽게 요약하고 사용자의 확인을 요청하세요.
                예: '지금까지 제가 이해한 내용을 정리해드릴게요. [정보 요약] 제가 이해한 내용이 맞나요?'
            "];
            return false;
        }

        return false;  // 아직 정보 수집 중
    }
    
    // GPT 응답 처리
    public function processResponse($promptData) {
        $chatHistory = $this->getChatHistory();
        $messages = [];
        $messages[] = ["role" => "system", "content" => $this->createSystemMessage($chatHistory)];

        foreach ($chatHistory as $chat) {
            if ($chat['user_data']) {
                $messages[] = ["role" => "user", "content" => $chat['user_data']];
            }
            if ($chat['gpt_data']) {
                $messages[] = ["role" => "assistant", "content" => $chat['gpt_data']];
            }
        }

        $messages[] = ["role" => "user", "content" => $promptData['prompt']];

        $status = $this->isReadyForSchedule($chatHistory);

        if ($status === 'need_confirmation') {
            // 수집된 정보 확인 요청
            $messages[] = ["role" => "system", "content" => "
                지금까지 수집된 정보를 자연스럽게 요약하고 사용자의 확인을 요청하세요.
                예: '지금까지 제가 이해한 내용을 정리해드릴게요. [정보 요약] 제가 이해한 내용이 맞나요?'
            "];
        } else if ($status === 'ready') {
            // JSON 형식으로 일정 생성
            $messages[] = ["role" => "system", "content" => "
                충분한 정보가 수집되었습니다. 이제 수집된 정보를 바탕으로 일정을 계획하세요.
                반드시 지정된 JSON 형식으로 응답하고, 각 장소의 정확한 주소와 위도/경도 정보를 포함하세요.
                제공된 장소 목록에 없는 장소는 place_api를 통해 검색하여 정보를 얻으세요.
                이제 일정을 JSON 형식으로만 응답하세요:
                {
                    \"response\": \"일반적인 응답 메시지\",
                    \"schedule\": {
                        \"1\": [{\"name\":\"장소1\",\"address\":\"주소1\",\"latitude\":\"위도1\",\"longitude\":\"경도1\"}],
                        \"2\": [{\"name\":\"장소2\",\"address\":\"주소2\",\"latitude\":\"위도2\",\"longitude\":\"경도2\"}]
                    }
                }
            "];
        }

        return $this->streamResponse($messages);
    }

    private function validateAndEnrichSchedule($content) {
        try {
            $scheduleData = json_decode($content, true);
            if (!isset($scheduleData['schedule'])) {
                error_log("Schedule data not found in GPT response");
                return null;
            }
    
            $isModified = false;

            foreach ($scheduleData['schedule'] as $day => &$places) {
                foreach ($places as &$place) {
                    // 장소명은 있지만 주소나 좌표가 없는 경우 처리
                    if (isset($place['name']) && (!isset($place['address']) || !isset($place['latitude']) || !isset($place['longitude']))) {
                        error_log("Fetching missing place details for: " . $place['name']);

                        // Place API로 주소와 좌표 정보 가져오기
                        $placeDetails = $this->getPlaceDetails($place['name']);
                        if ($placeDetails) {
                            $place['address'] = $placeDetails['address'];
                            $place['latitude'] = (string)$placeDetails['latitude'];
                            $place['longitude'] = (string)$placeDetails['longitude'];
                            $isModified = true;
                            error_log("Place details enriched for: " . $place['name']);
                        } else {
                            error_log("Failed to find place details for: " . $place['name']);
                            // 실패 시 null 반환하여 GPT에게 다시 요청
                            return null;
                        }
                    }
                        
                    // 좌표값이 문자열이 아닌 경우 변환
                    if (isset($place['latitude']) && !is_string($place['latitude'])) {
                        $place['latitude'] = (string)$place['latitude'];
                        $isModified = true;
                    }
                    if (isset($place['longitude']) && !is_string($place['longitude'])) {
                        $place['longitude'] = (string)$place['longitude'];
                        $isModified = true;
                    }
                }
            }
    
            if ($isModified) {
                return json_encode($scheduleData);
            }

            return $content;
        } catch (Exception $e) {
            error_log("Error validating schedule: " . $e->getMessage());
            return null;
        }
    }
    
    
    // 스트림 응답 처리
    private function streamResponse($messages) {
        error_log("[Stream] Starting stream response");
        $userId = $this->getUserId();
        error_log("[Stream] User ID: " . ($userId ?? 'null'));

        if ($userId) {
            $this->saveChatHistory($userId, 0, $messages[count($messages)-1]['content'], null);
        }

        $requestData = [
            "model" => "gpt-4o",
            "stream" => true,
            "messages" => $messages
        ];

        $ch = curl_init('https://api.openai.com/v1/chat/completions');
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => false,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'Authorization: Bearer ' . $this->apiKey
            ],
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($requestData)
        ]);
    
        error_log("Request to GPT: " . json_encode($requestData));

        $streamData = '';
        curl_setopt($ch, CURLOPT_WRITEFUNCTION, function($ch, $data) use (&$streamData) {
            if ($this->isReadyForSchedule($this->getChatHistory())) {
                // JSON 응답이 완성될 때까지 데이터 수집
                $streamData .= $data;
                return strlen($data);
            } else {
                // 일반 대화 모드일 때는 바로 출력
                echo $data;
                ob_flush();
                flush();
                $streamData .= $data;
                return strlen($data);
            }
        });
    
        error_log("Response from GPT: " . $streamData);

        ob_start();
        curl_exec($ch);
        curl_close($ch);

        // 일정 계획 모드일 경우
        if ($this->isReadyForSchedule($this->getChatHistory())) {
            $content = $this->parseStreamData($streamData);
            $maxRetries = 3;
            $retryCount = 0;

            while ($retryCount < $maxRetries) {
                $validatedContent = $this->validateAndEnrichSchedule($content);

                if ($validatedContent) {
                    echo $validatedContent;
                    ob_flush();
                    flush();

                    if ($userId) {
                        $this->saveChatHistory($userId, 1, null, $validatedContent);
                    }
                    return;
                }

                // 장소 정보 검증 실패 시 GPT에게 다시 요청
                $messages[] = ["role" => "system", "content" => "
                    이전 응답의 일부 장소를 찾을 수 없습니다.
                    더 정확한 장소명을 사용하여 다시 일정을 작성해주세요.
                    실제 존재하는 관광지나 명소의 정확한 이름을 사용해주세요.
                "];

                $retryCount++;
                if ($retryCount >= $maxRetries) {
                    echo json_encode([
                        "response" => "죄송합니다. 일부 장소를 찾을 수 없습니다. 다른 장소로 일정을 다시 작성해드릴까요?",
                        "schedule" => null
                    ]);
                    break;
                }

                // GPT에 다시 요청
                $requestData["messages"] = $messages;
                $ch = curl_init('https://api.openai.com/v1/chat/completions');
                curl_setopt_array($ch, [
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_HTTPHEADER => [
                        'Content-Type: application/json',
                        'Authorization: Bearer ' . $this->apiKey
                    ],
                    CURLOPT_POST => true,
                    CURLOPT_POSTFIELDS => json_encode($requestData)
                ]);

                $response = curl_exec($ch);
                curl_close($ch);

                $result = json_decode($response, true);
                if (isset($result['choices'][0]['message']['content'])) {
                    $content = $result['choices'][0]['message']['content'];
                } else {
                    break;
                }
            }
        } else {
            // 일반 대화 모드
            if ($userId) {
                $cleanContent = $this->parseStreamData($streamData);
                $this->saveChatHistory($userId, 1, null, $cleanContent);
            }
        }
    }
    
    // 사용자 ID 가져오기
    private function getUserId() {
        if (!$this->conn || !$this->userEmail) return null;
        $stmt = $this->conn->prepare("SELECT user_id FROM User WHERE user_email = :email");
        $stmt->execute(['email' => $this->userEmail]);
        return $stmt->fetchColumn();
    }
    
    // 채팅 기록 저장
    private function saveChatHistory($userId, $who, $userData, $gptData) {
        $stmt = $this->conn->prepare("
            INSERT INTO chat_history (user_id, session_id, who, user_data, gpt_data, created_at)
            VALUES (:user_id, :session_id, :who, :user_data, :gpt_data, CURRENT_TIMESTAMP)
        ");

        $stmt->execute([
            'user_id' => $userId,
            'session_id' => $this->sessionId,
            'who' => $who,
            'user_data' => $userData,
            'gpt_data' => $gptData
        ]);
    }
    
    // 스트림 데이터 파싱
    private function parseStreamData($streamData) {
        $messages = explode("data: ", $streamData);
        $content = '';

        foreach ($messages as $message) {
            if (empty(trim($message))) continue;
            if ($message === "[DONE]") continue;

            $data = json_decode($message, true);
            if (!$data) continue;

            if (isset($data['choices'][0]['delta']['content'])) {
                $content .= $data['choices'][0]['delta']['content'];
            }
        }

        return $content;
    }
}

// 메인 실행 코드
$totalStartTime = microtime(true);

// JSON 입력 처리
$json = file_get_contents('php://input');
$data = json_decode($json, true);

if (json_last_error() !== JSON_ERROR_NONE) {
    die(json_encode(["status" => "error", "message" => "Invalid JSON data."]));
}

$promptData = json_decode($data['prompt'], true);
if (!$promptData) {
    die(json_encode(["status" => "error", "message" => "Invalid prompt data."]));
}

// 환경 변수 설정
$apiKey = getenv('OPENAI_API_KEY');
$placeApiKey = "YOUR_GOOGLE_API_KEY";

if (!$apiKey) {
    die(json_encode(["status" => "error", "message" => "OpenAI API key not found"]));
}

// DB 연결
$servername = "YOUR_DB_HOST";
$username = "YOUR_DB_USER";
$password = "YOUR_DB_PASSWORD";
$dbname = "rubato_db";

try {
    $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $conn->exec("SET NAMES utf8mb4");
} catch(PDOException $e) {
    error_log("Database connection failed: " . $e->getMessage());
    $conn = null;
}

// TravelPlanner 인스턴스 생성 및 실행
try {
    $planner = new TravelPlanner(
        $conn, 
        $apiKey,
        $data['session_id'] ?? null,
        $data['user_email'] ?? null,
        $placeApiKey
    );

    // 새로운 세션인 경우 초기화
    if (isset($data['new_session']) && $data['new_session']) {
        $planner->clearSession();
    }


    $planner->processResponse($promptData);
} catch (Exception $e) {
    error_log("Travel planner error: " . $e->getMessage());
    die(json_encode([
        "status" => "error",
        "message" => "An error occurred while processing your request."
    ]));
} finally {
    // 실행 시간 로깅
    $totalEndTime = microtime(true);
    error_log(sprintf(
        "Total Execution Time: %.2f seconds, Session: %s",
        $totalEndTime - $totalStartTime,
        $data['session_id'] ?? 'unknown'
    ));
    
    // DB 연결 종료
    $conn = null;
}
?>