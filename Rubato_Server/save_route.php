<?php
$openaiApiKey = getenv('OPENAI_API_KEY');
$geminiApiKey = 'YOUR_GOOGLE_API_KEY';
$servername = "YOUR_DB_HOST";
$username = "YOUR_DB_USER";
$password = "YOUR_DB_PASSWORD";
$dbname = "rubato_db";

// 데이터베이스 연결
$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die(json_encode(array("status" => "error", "message" => "DB 연결 실패: " . $conn->connect_error)));
}

// JSON 요청 데이터 수신
$data = file_get_contents("php://input");
$request = json_decode($data, true);

error_log("Received data: " . print_r($request, true));

// user_email로 user_id 조회
$userQuery = "SELECT user_id FROM User WHERE user_email = ?";
$stmt = $conn->prepare($userQuery);
$userEmail = $request['user_email'];
$stmt->bind_param("s", $userEmail);
$stmt->execute();
$result = $stmt->get_result();

if (!$result->num_rows) {
    die(json_encode(array("status" => "error", "message" => "User not found with email: " . $userEmail)));
}
$userId = $result->fetch_assoc()['user_id'];

// 태그 및 테마 조회
$tags = [];
$tagQuery = "SELECT tag_id, tag_name FROM Tag";
$tagResult = $conn->query($tagQuery);
while($row = $tagResult->fetch_assoc()) {
    $tags[$row['tag_id']] = $row['tag_name'];
}

$themes = [];
$themeQuery = "SELECT theme_id, theme_name FROM Theme";
$themeResult = $conn->query($themeQuery);
while($row = $themeResult->fetch_assoc()) {
    $themes[$row['theme_id']] = $row['theme_name'];
}

// Gemini API를 사용하는 병렬 처리 클래스
class ParallelGeminiProcessor {
    private $geminiApiKey;
    private $maxConcurrent;
    private $requests = [];
    
    public function __construct($geminiApiKey, $maxConcurrent = 5) {
        $this->geminiApiKey = $geminiApiKey;
        $this->maxConcurrent = $maxConcurrent;
    }
    
    public function addRequest($placeName) {
        $this->requests[] = [
            'place_name' => $placeName,
            'attempt' => 1,
            'cost' => 0
        ];
    }

    public function processBatch() {
        $results = [];
        $mh = curl_multi_init();
        $running = null;
        $batch = [];

        while (!empty($this->requests) || !empty($batch)) {
            while (count($batch) < $this->maxConcurrent && !empty($this->requests)) {
                $request = array_shift($this->requests);
                $ch = $this->createCurlHandle($request['place_name']);
                curl_multi_add_handle($mh, $ch);
                $batch[(int)$ch] = [
                    'ch' => $ch,
                    'place_name' => $request['place_name'],
                    'attempt' => $request['attempt']
                ];
            }

            do {
                $status = curl_multi_exec($mh, $running);
            } while ($status === CURLM_CALL_MULTI_PERFORM);

            while ($completed = curl_multi_info_read($mh)) {
                $ch = $completed['handle'];
                $handle_id = (int)$ch;

                $response = curl_multi_getcontent($ch);
                $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                $placeName = $batch[$handle_id]['place_name'];
                $attempt = $batch[$handle_id]['attempt'];

                $cost = $this->processResponse($response, $httpCode, $placeName);

                if ($cost > 0 && $attempt == 1) {
                    $this->addVerificationRequest($placeName, $cost);
                } elseif ($attempt == 1 && $cost == 0 && $attempt < 3) {
                    $this->requests[] = [
                        'place_name' => $placeName,
                        'attempt' => $attempt + 1,
                        'cost' => 0
                    ];
                } else {
                    $results[$placeName] = $cost;
                }

                curl_multi_remove_handle($mh, $ch);
                curl_close($ch);
                unset($batch[$handle_id]);
            }

            if ($running > 0) {
                curl_multi_select($mh);
            }
        }

        curl_multi_close($mh);
        return $results;
    }

private function createCurlHandle($placeName) {
        $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" . $this->geminiApiKey;

        $data = [
            "contents" => [
                [
                    "parts" => [
                        ["text" => "인터넷에서 검색해서 다음 장소의 가격 정보를 찾아주세요: {$placeName}의 입장료나 평균 식사 비용
                        반드시 다음 조건을 지켜주세요:
                        - 반드시 실제 인터넷 검색 결과를 바탕으로 응답해주세요
                        - 정확한 금액을 원화로 숫자만 알려주세요 (예: 9000 또는 62000)
                        - 입장료가 있는 곳이면 성인 기준 입장료
                        - 음식점이면 1인당 평균 식사 비용
                        - 무료인 경우 0
                        - 다른 설명은 하지 말고 숫자만 응답해주세요"]
                    ]
                ]
            ],
            "tools" => [
                "google_search_retrieval" => new stdClass()
            ],
            "safetySettings" => [
                [
                    "category" => "HARM_CATEGORY_DANGEROUS_CONTENT",
                    "threshold" => "BLOCK_NONE"
                ]
            ],
            "generationConfig" => [
                "temperature" => 0.1,
                "topK" => 1,
                "topP" => 0.1,
                "maxOutputTokens" => 100
            ]
        ];

        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json'
            ],
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($data)
        ]);

        return $ch;
    }

    private function processResponse($response, $httpCode, $placeName) {
        if ($httpCode !== 200) {
            error_log("API Error for {$placeName} - HTTP " . $httpCode);
            return 0;
        }

        $responseData = json_decode($response, true);
        if (isset($responseData['candidates'][0]['content']['parts'][0]['text'])) {
            $text = strtolower(trim($responseData['candidates'][0]['content']['parts'][0]['text']));
            error_log("Raw response text for {$placeName}: " . $text);

            preg_match('/\d+/', $text, $matches);
            if (!empty($matches)) {
                $cost = (int)$matches[0];
                error_log("Cost found for {$placeName}: {$cost}원");
                return $cost;
            }
        }
        return 0;
    }

    private function addVerificationRequest($placeName, $cost) {
        $this->requests[] = [
            'place_name' => $placeName,
            'attempt' => 2,
            'cost' => $cost
        ];
    }
}

// 병렬 처리기 초기화 및 요청 추가
$processor = new ParallelGeminiProcessor($geminiApiKey);

// 모든 장소를 한번에 추가
foreach ($request['route_details'] as $dayDetail) {
    if (isset($dayDetail['places'])) {
        foreach ($dayDetail['places'] as $place) {
            $processor->addRequest($place['place_name']);
        }
    }
}

// 병렬 처리 실행 및 결과 수집
$placeCosts = $processor->processBatch();

// 비용 계산
$totalCost = 0;
$dailyCosts = array_fill(0, count($request['route_details']), 0);

foreach ($request['route_details'] as $dayIndex => $dayDetail) {
    if (isset($dayDetail['places'])) {
        foreach ($dayDetail['places'] as $place) {
            $cost = $placeCosts[$place['place_name']] ?? 0;
            $totalCost += $cost;
            $dailyCosts[$dayIndex] += $cost;
        }
    }
}

// 여행 경로 설명 생성
$routeDescription = "다음은 {$request['total_days']}일 동안의 여행 경로입니다:\n";
foreach ($request['route_details'] as $dayIndex => $dayDetail) {
    if (isset($dayDetail['places'])) {
        $routeDescription .= ($dayIndex + 1) . "일차: ";
        foreach ($dayDetail['places'] as $place) {
            $routeDescription .= $place['place_name'] . ", ";
        }
        $routeDescription .= "\n";
    }
}

// GPT를 사용한 태그 분석
$ch = curl_init("https://api.openai.com/v1/chat/completions");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        "Authorization: Bearer $openaiApiKey",
        "Content-Type: application/json"
    ],
    CURLOPT_POSTFIELDS => json_encode([
        "model" => "gpt-4o",
        "messages" => [
            ["role" => "system", "content" => "You are a helpful assistant that analyzes travel routes."],
            ["role" => "user", "content" => "다음 여행 경로를 분석하고, 다음 태그 중에서 적절한 태그를 선택해주세요:\n" . 
                                          implode(", ", $tags) . "\n\n" . $routeDescription . 
                                          "\n가장 적절한 태그 3개만 태그 이름으로 응답해주세요."]
        ]
    ])
]);

$response = curl_exec($ch);
curl_close($ch);

$selectedTags = ["일반"]; // 기본값 설정
if ($response !== false) {
    $gptResponse = json_decode($response, true);
    if ($gptResponse && 
        isset($gptResponse['choices'][0]['message']['content'])) {
        $content = $gptResponse['choices'][0]['message']['content'];
        if (!empty($content)) {
            $selectedTags = explode(",", $content);
            $selectedTags = array_map('trim', $selectedTags);
        }
    }
}

// 테마 선택을 위한 GPT 요청
$ch = curl_init("https://api.openai.com/v1/chat/completions");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        "Authorization: Bearer $openaiApiKey",
        "Content-Type: application/json"
    ],
    CURLOPT_POSTFIELDS => json_encode([
        "model" => "gpt-4o",
        "messages" => [
            ["role" => "system", "content" => "You are a helpful assistant that determines travel themes based on travel routes and tags."],
            ["role" => "user", "content" => "다음 여행 경로와 태그들을 바탕으로 여행의 테마를 선택해주세요:\n\n" .
                                          "여행 경로:\n" . $routeDescription . "\n\n" .
                                          "선택된 태그: " . implode(", ", $selectedTags) . "\n\n" .
                                          "다음 테마 중에서 선택해주세요:\n" . implode(", ", $themes) . 
                                          "\n\n가장 적절한 테마 하나만 테마 이름으로 응답해주세요."]
        ]
    ])
]);

$response = curl_exec($ch);
curl_close($ch);

$themeId = 1; // 기본값 설정
if ($response !== false) {
    $gptResponse = json_decode($response, true);
    if ($gptResponse && isset($gptResponse['choices'][0]['message']['content'])) {
        $selectedTheme = trim($gptResponse['choices'][0]['message']['content']);
        $foundThemeId = array_search($selectedTheme, $themes);
        if ($foundThemeId !== false) {
            $themeId = $foundThemeId;
        }
    }
}

// 경로 점수 계산 함수
function calculateRouteScore($totalCost, $totalDistance, $placesCount) {
    $costScore = min(40, (1000000 - min($totalCost, 1000000)) / 25000);
    $distanceScore = min(30, (100 - min($totalDistance, 100)) / 3.33);
    $placeScore = min(30, $placesCount * 5);
    return (int)($costScore + $distanceScore + $placeScore);
}

// 트랜잭션 시작
$conn->begin_transaction();

try {
    $totalPlaces = 0;
    foreach ($request['route_details'] as $dayDetail) {
        if (isset($dayDetail['places'])) {
            $totalPlaces += count($dayDetail['places']);
        }
    }
    
    $routeScore = calculateRouteScore($totalCost, $request['total_distance'], $totalPlaces);

    // 이미지 데이터 처리
    $routeImage = null;
    if (!empty($request['route_image'])) {
        if (strpos($request['route_image'], 'base64,') !== false) {
            list(, $base64Data) = explode('base64,', $request['route_image']);
        } else {
            $base64Data = $request['route_image'];
        }
        $routeImage = base64_decode($base64Data);
    }

    // Route 정보 저장
    $routeQuery = "INSERT INTO Route (user_id, theme_id, likes, final_score, estimated_cost, total_distance, route_image) 
                  VALUES (?, ?, ?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($routeQuery);
    if (!$stmt) {
        throw new Exception("Failed to prepare route insert statement: " . $conn->error);
    }
    
    $likes = 0;
    $stmt->bind_param("iiiddds", $userId, $themeId, $likes, $routeScore, $totalCost, $request['total_distance'], $routeImage);
    
    if (!$stmt->execute()) {
        throw new Exception("Failed to insert route: " . $stmt->error);
    }
    $routeId = $stmt->insert_id;

    // 경로 상세 정보 저장
    foreach ($request['route_details'] as $dayDetail) {
        if (isset($dayDetail['places'])) {
            $date = $dayDetail['date'];
            foreach ($dayDetail['places'] as $placeIndex => $place) {
                $detailQuery = "INSERT INTO Route_detail (route_id, place_name, place_latitude, place_longitude, date, route_seq) 
                              VALUES (?, ?, ?, ?, ?, ?)";
                $stmt = $conn->prepare($detailQuery);
                $seq = $placeIndex + 1;

                $stmt->bind_param("isddsi", $routeId, $place['place_name'], $place['latitude'],
                                $place['longitude'], $date, $seq);
                $stmt->execute();
            }
        }
    }

// 태그 정보 저장
    foreach ($selectedTags as $tagName) {
        $tagId = array_search($tagName, $tags);
        if ($tagId !== false) {
            $tagQuery = "INSERT INTO Route_Tag (route_id, tag_id) VALUES (?, ?)";
            $stmt = $conn->prepare($tagQuery);
            $stmt->bind_param("ii", $routeId, $tagId);
            $stmt->execute();
        }
    }

    // 지역 정보 저장
    foreach ($request['areas'] as $areaName) {
        $areaQuery = "SELECT area_id FROM Area WHERE area_name = ?";
        $stmt = $conn->prepare($areaQuery);
        $stmt->bind_param("s", $areaName);
        $stmt->execute();
        $areaResult = $stmt->get_result();

        if ($areaResult->num_rows > 0) {
            $areaId = $areaResult->fetch_assoc()['area_id'];
            $routeAreaQuery = "INSERT INTO Route_Area (route_id, area_id) VALUES (?, ?)";
            $stmt = $conn->prepare($routeAreaQuery);
            $stmt->bind_param("ii", $routeId, $areaId);
            $stmt->execute();
        }
    }

    $conn->commit();

    // 응답 생성
    $response = [
        "status" => "success",
        "route_summary" => [
            "route_id" => $routeId,
            "theme" => $themes[$themeId],
            "tags" => $selectedTags,
            "total_cost" => $totalCost,
            "daily_costs" => $dailyCosts,
            "total_distance" => $request['total_distance'],
            "route_score" => $routeScore,
            "image_saved" => ($routeImage !== null),
            "daily_details" => array_map(function($dayDetail, $dayCost) use ($placeCosts, $request) {
                $dailyDistances = isset($dayDetail['distances']) ? $dayDetail['distances'] : [];
                $totalDailyDistance = 0;
                foreach ($dailyDistances as $distance) {
                    $totalDailyDistance += (float)str_replace(' km', '', $distance);
                }

                return [
                    "day" => $dayDetail['day'],
                    "date" => $dayDetail['date'],
                    "places" => isset($dayDetail['places']) ? array_map(function($place) use ($placeCosts) {
                        return [
                            "name" => $place['place_name'],
                            "estimated_cost" => $placeCosts[$place['place_name']] ?? 0
                        ];
                    }, $dayDetail['places']) : [],
                    "distances" => $dailyDistances,
                    "day_total_cost" => $dayCost,
                    "total_distance" => $totalDailyDistance
                ];
            }, $request['route_details'], $dailyCosts)
        ]
    ];

    header("Content-Type: application/json");
    echo json_encode($response);

} catch (Exception $e) {
    $conn->rollback();
    error_log("Error in save_route.php: " . $e->getMessage());
    
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => $e->getMessage()
    ]);
}

$conn->close();
?>