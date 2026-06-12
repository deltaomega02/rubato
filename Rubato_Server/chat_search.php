<?php
# chat_search.php
// ParallelGeminiProcessor 클래스 정의
class ParallelGeminiProcessor {
    private $geminiApiKey;
    private $maxConcurrent;
    private $requests = [];
    private $cache = [];
    private $cacheFile = '/tmp/gemini_cache.json';
    
    public function __construct($geminiApiKey, $maxConcurrent = 5) {
        $this->geminiApiKey = $geminiApiKey;
        $this->maxConcurrent = $maxConcurrent;
        $this->loadCache();
    }
    
    private function loadCache() {
        if (file_exists($this->cacheFile)) {
            $this->cache = json_decode(file_get_contents($this->cacheFile), true) ?: [];
        }
    }
    
    private function saveCache() {
        file_put_contents($this->cacheFile, json_encode($this->cache));
    }
    
    public function addRequest($keyword, $options = []) {
        $categories = isset($options['categories']) ? $options['categories'] : ['기본정보', '이용정보'];
        $context = isset($options['context']) ? $options['context'] : null;

        foreach ($categories as $category) {
            $queries = $this->generateQueriesForCategory($keyword, $category);
            foreach ($queries as $query) {
                $queryKey = md5($query);
                if (!isset($this->cache[$queryKey]) || 
                    (time() - $this->cache[$queryKey]['timestamp'] > 86400)) {
                    $this->requests[] = [
                        'keyword' => $keyword,
                        'query' => $query,
                        'category' => $category,
                        'attempt' => 1
                    ];
                }
            }
        }
    }
    
    private function generateQueriesForCategory($keyword, $category) {
        $queries = [];

        $categoryQueries = [
            '기본정보' => ['개요', '특징', '규모'],
            '이용정보' => ['영업시간', '입장료', '할인정보'],
            '교통정보' => ['주소', '주차장', '대중교통'],
            '관광정보' => ['주요명소', '추천코스', '볼거리'],
            '편의정보' => ['맛집', '카페', '편의시설'],
            '이벤트' => ['행사일정', '축제정보', '공연'],
            '날씨' => ['날씨정보', '계절별특징'],
            '안전정보' => ['안전수칙', '이용규정', '연락처'],
            '방문자리뷰' => ['방문후기', '평점', '추천의견']
        ];

        if (isset($categoryQueries[$category])) {
            foreach ($categoryQueries[$category] as $subQuery) {
                $queries[] = $keyword . ' ' . $subQuery;
            }
        }

        return $queries;
    }
    
    public function processBatch() {
        $batchStartTime = microtime(true);
        $results = [];
        $mh = curl_multi_init();
        $batch = [];
        $activeRequests = [];

        while (!empty($this->requests) || !empty($batch)) {
            while (count($batch) < $this->maxConcurrent && !empty($this->requests)) {
                $request = array_shift($this->requests);
                $ch = $this->createCurlHandle($request['keyword'], $request['query']);
                curl_multi_add_handle($mh, $ch);
                $batch[(int)$ch] = $request;
            }

            do {
                $status = curl_multi_exec($mh, $running);
            } while ($status === CURLM_CALL_MULTI_PERFORM);

            while ($completed = curl_multi_info_read($mh)) {
                $ch = $completed['handle'];
                $handle_id = (int)$ch;

                $response = curl_multi_getcontent($ch);
                $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                $request = $batch[$handle_id];

                $processedResponse = $this->processResponse($response, $httpCode);
                if ($processedResponse) {
                    $queryKey = md5($request['query']);
                    $this->cache[$queryKey] = [
                        'response' => $processedResponse,
                        'timestamp' => time()
                    ];

                    if (!isset($results[$request['keyword']])) {
                        $results[$request['keyword']] = [];
                    }
                    if (!isset($results[$request['keyword']][$request['category']])) {
                        $results[$request['keyword']][$request['category']] = [];
                    }
                    $results[$request['keyword']][$request['category']][] = [
                        'query' => $request['query'],
                        'response' => $processedResponse
                    ];
                }

                curl_multi_remove_handle($mh, $ch);
                curl_close($ch);
                unset($batch[$handle_id]);
            }

            if ($running > 0) {
                curl_multi_select($mh, 1.0);
            }
        }

        curl_multi_close($mh);
        $this->saveCache();

        return $this->organizeResults($results);
    }
    
    private function createCurlHandle($keyword, $query) {
        $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" . $this->geminiApiKey;

        $prompt = "'{$query}'에 대해 최신 정보를 검색해주세요. 핵심 정보만 간단명료하게 알려주세요.";

        $data = [
            "contents" => [
                [
                    "parts" => [
                        ["text" => $prompt]
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
            CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($data),
            CURLOPT_TIMEOUT => 10
        ]);

        return $ch;
    }
    
    private function processResponse($response, $httpCode) {
        if ($httpCode !== 200) {
            error_log("Gemini API Error - HTTP " . $httpCode);
            return null;
        }

        try {
            $responseData = json_decode($response, true);
            if (!$responseData || 
                !isset($responseData['candidates']) || 
                !isset($responseData['candidates'][0]['content']['parts'][0]['text'])) {
                error_log("Invalid Gemini response structure");
                return null;
            }

            return $responseData['candidates'][0]['content']['parts'][0]['text'];
        } catch (Exception $e) {
            error_log("Response processing error: " . $e->getMessage());
            return null;
        }
    }
    
    private function organizeResults($results) {
        $organized = [];

        foreach ($results as $keyword => $categoryResults) {
            $organized[$keyword] = [];
            foreach ($categoryResults as $category => $items) {
                $organized[$keyword][$category] = [];
                foreach ($items as $item) {
                    $queryParts = explode(' ', $item['query']);
                    $subtype = end($queryParts);
                    $organized[$keyword][$category][$subtype] = $item['response'];
                }
            }
        }

        return $organized;
    }
}

// 키워드 추출 로직부터 시작
$keywordStartTime = microtime(true);
$keywordMessages = array();
$keywordMessages[] = array("role" => "system", "content" => "사용자의 질문을 분석하여 가장 필요한 정보 카테고리 2개만 선택해주세요.

사용 가능한 카테고리:
1. 기본정보 (개요, 특징, 규모, 역사)
2. 이용정보 (영업시간, 입장료, 할인정보)
3. 교통정보 (주소, 주차장, 대중교통)
4. 관광정보 (명소, 코스, 볼거리)
5. 편의정보 (맛집, 카페, 쇼핑)
6. 이벤트 (행사, 축제, 공연)
7. 날씨 (기후, 계절별 특징)
8. 안전정보 (규정, 준비물, 연락처)
9. 방문자리뷰 (후기, 평점)

다음 JSON 형식으로만 응답하세요:
{
    \"categories\": [\"카테고리1\", \"카테고리2\"],
    \"keywords\": [\"검색키워드1\", \"검색키워드2\"]
}");
$keywordMessages[] = array("role" => "user", "content" => $promptData['prompt']);

$keywordResponse = callGPTForKeywords($keywordMessages, $apiKey);
$keywordData = json_decode($keywordResponse, true);

// Gemini 검색 실행
$geminiProcessor = new ParallelGeminiProcessor($geminiApiKey);
foreach ($keywordData['keywords'] as $keyword) {
    $geminiProcessor->addRequest($keyword, [
        'categories' => $keywordData['categories'],
        'context' => $promptData['prompt']
    ]);
}

$searchResults = $geminiProcessor->processBatch();

// 최종 응답 준비 및 생성
$messages = array();
$messages[] = array("role" => "system", "content" => 
    "당신은 여행 전문가입니다. 제공받은 실시간 검색 결과를 바탕으로 사용자의 질문에 답변해주세요.
    - 가격, 운영시간, 위치 등 구체적인 정보는 반드시 포함해주세요
    - 검색 결과의 출처도 '~에 따르면' 형식으로 간단히 언급해주세요
    - 정확한 수치나 정보는 반드시 그대로 전달해주세요
    - 불확실한 정보는 '~로 추정됩니다', '~인 것으로 보입니다' 등으로 표현해주세요
    - 검색 결과가 없거나 불명확한 경우 솔직히 말씀해주세요");
$messages[] = array("role" => "system", "content" => "Please always answer in Korean.");
$messages[] = array("role" => "user", "content" => "질문: " . $promptData['prompt'] . "\n검색결과: " . json_encode($searchResults, JSON_UNESCAPED_UNICODE));

streamResponse($messages, $apiKey);

// 필요한 함수들
function callGPTForKeywords($messages, $apiKey) {
    $ch = curl_init('https://api.openai.com/v1/chat/completions');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey
    ));
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(array(
        "model" => "gpt-4o",
        "messages" => $messages
    )));

    $response = curl_exec($ch);
    curl_close($ch);

    $result = json_decode($response, true);
    return $result['choices'][0]['message']['content'];
}

function streamResponse($messages, $apiKey) {
    $requestData = array(
        "model" => "gpt-4o",
        "stream" => true,
        "messages" => $messages
    );

    $ch = curl_init('https://api.openai.com/v1/chat/completions');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, false);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey
    ));
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($requestData));

    $streamData = '';
    curl_setopt($ch, CURLOPT_WRITEFUNCTION, function($ch, $data) use (&$streamData) {
        echo $data;
        ob_flush();
        flush();
        $streamData .= $data;
        return strlen($data);
    });

    ob_start();
    $result = curl_exec($ch);
    ob_end_flush();
    curl_close($ch);
}
?>