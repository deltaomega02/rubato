<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
ini_set('log_errors', 1);
ini_set('error_log', '/var/log/apache2/place_api_error.log');
ini_set('max_execution_time', 300);
set_time_limit(300);

// 로깅 함수
function writeLog($message, $type = 'INFO') {
    $logMessage = date('Y-m-d H:i:s') . " [$type] " . $message . "\n";
    error_log($logMessage);
}

header("Content-Type: application/json; charset=UTF-8");

// 설정 변수
//$googleApiKey = "YOUR_GOOGLE_API_KEY";
$googleApiKey = "YOUR_GOOGLE_API_KEY";
$typeList = ["tourist_attraction", "restaurant"];
$maxConcurrentRequests = 10;
$maxResultsPerType = 15; // 각 타입당 최대 결과 수 (3페이지 * 20개)

// OpenAI API 키 설정
$apiKey = getenv('OPENAI_API_KEY');
if (!$apiKey) {
    writeLog("API key not found", 'ERROR');
    die(json_encode(["status" => "error", "message" => "API key not found"]));
}

// 입력 데이터 검증
$rawInput = file_get_contents("php://input");
writeLog("Received raw input: " . $rawInput);

if (!$rawInput) {
    writeLog("No JSON input received", 'ERROR');
    die(json_encode(["status" => "error", "message" => "No input data provided."]));
}

$inputData = json_decode($rawInput, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    writeLog("JSON decode error: " . json_last_error_msg(), 'ERROR');
    die(json_encode(["status" => "error", "message" => "Invalid JSON data."]));
}

$selectedLocations = $inputData["selectedLocations"] ?? [];
writeLog("Selected locations: " . implode(", ", $selectedLocations));

// DB 연결 및 태그 가져오기
try {
    $conn = new mysqli("YOUR_DB_HOST", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "rubato_db");
    if ($conn->connect_error) {
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }
    
    $tags = [];
    $result = $conn->query("SELECT tag_name FROM Tag");
    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $tags[] = $row["tag_name"];
        }
        writeLog("Retrieved " . count($tags) . " tags from database");
    }
} catch (Exception $e) {
    writeLog("Database error: " . $e->getMessage(), 'ERROR');
    die(json_encode(["status" => "error", "message" => "Database operation failed."]));
}

// 장소 유형 매핑 배열
$placeTypeMap = [
    // 기본 매핑
    'restaurant' => '레스토랑',
    'cafe' => '카페/디저트',
    'tourist_attraction' => '관광명소',
    'point_of_interest' => '관광명소',
    'natural_feature' => '자연경관',
    'park' => '공원/정원',
    'museum' => '박물관/전시관',
    'art_gallery' => '박물관/전시관',
    'amusement_park' => '체험/테마파크',
    'aquarium' => '체험/테마파크',
    'food' => '한식/일식',

    // 키워드 기반 매핑
    '해변' => '해변/바다',
    '바다' => '해변/바다',
    '해수욕장' => '해변/바다',
    '폭포' => '자연경관',
    '계곡' => '자연경관',
    '오름' => '산/오름',
    '산' => '산/오름',
    '올레' => '산/트래킹',
    '박물관' => '박물관/전시관',
    '미술관' => '박물관/전시관',
    '공원' => '공원/정원',
    '정원' => '공원/정원',
    '성' => '역사유적',
    '궁' => '역사유적',
    '레스토랑' => '레스토랑',
    '식당' => '한식/일식',
    '카페' => '카페/디저트',
    '커피' => '카페/디저트',
    '타워' => '랜드마크',
    '전망대' => '랜드마크',
    '굴' => '자연동굴/지형',
    '동굴' => '자연동굴/지형',
    '체험' => '체험/테마파크',

    '체험' => '체험/테마파크',
    '테마파크' => '체험/테마파크',
    '랜드' => '체험/테마파크',
    '월드' => '체험/테마파크',
    '놀이동산' => '체험/테마파크',
    '박물관' => '체험/테마파크',
    '아쿠아리움' => '체험/테마파크',
    '돔베누스' => '체험/테마파크',
    '헬로키티' => '체험/테마파크',
    
    '박물관' => '박물관/전시관',
    '전시관' => '박물관/전시관',
    '미술관' => '박물관/전시관',
    '기념관' => '박물관/전시관',
    '테지움' => '박물관/전시관',
    '플레이케이팝' => '박물관/전시관',

    '성' => '역사유적',
    '궁' => '역사유적',
    '관아' => '역사유적',
    '문화재' => '역사유적',
    '유적지' => '역사유적'
];

// Places API 페이지네이션 함수
function fetchPlacesWithPagination($location, $type, $googleApiKey, $maxResults = 60) {
    $places = [];
    $pageToken = null;
    $retryCount = 0;
    $maxRetries = 3;
    
    do {
        $url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" .
               "query=" . urlencode($type) . "+in+" . urlencode($location) .
               "&language=ko&key=" . $googleApiKey;

        if ($pageToken) {
            $url .= "&pagetoken=" . $pageToken;
            sleep(2); // Places API requires a delay between pagetoken requests
        }

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode !== 200) {
            writeLog("API request failed with HTTP code: $httpCode", 'ERROR');
            if (++$retryCount >= $maxRetries) {
                break;
            }
            sleep(2);
            continue;
        }

        $result = json_decode($response, true);

        if (isset($result["results"])) {
            $places = array_merge($places, $result["results"]);
            writeLog("Fetched " . count($result["results"]) . " places for $location ($type)");
        }

        $pageToken = $result["next_page_token"] ?? null;

        if (count($places) >= $maxResults) {
            $places = array_slice($places, 0, $maxResults);
            break;
        }
    } while ($pageToken && count($places) < $maxResults);
    
    return $places;
}

// 병렬 HTTP 요청 처리 함수
function makeParallelRequests($requests, $maxConcurrent = 10) {
    $multiHandle = curl_multi_init();
    $running = null;
    $responses = [];
    $handles = [];
    
    foreach ($requests as $index => $request) {
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $request['url'],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_ENCODING => ''
        ]);

        if (isset($request['headers'])) {
            curl_setopt($ch, CURLOPT_HTTPHEADER, $request['headers']);
        }
        if (isset($request['post_data'])) {
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $request['post_data']);
        }

        curl_multi_add_handle($multiHandle, $ch);
        $handles[$index] = $ch;
    }
    
    do {
        $status = curl_multi_exec($multiHandle, $running);
        if ($running) {
            curl_multi_select($multiHandle);
        }
    } while ($running && $status == CURLM_OK);
    
    foreach ($handles as $index => $ch) {
        $response = curl_multi_getcontent($ch);
        $responses[$index] = $response ? json_decode($response, true) : null;
        curl_multi_remove_handle($multiHandle, $ch);
    }
    
    curl_multi_close($multiHandle);
    return $responses;
}

// 장소 유형 결정 함수
function getDetailedPlaceType($place, $placeTypeMap) {
    $name = $place['name'];
    
    // 장소명 기반 매칭
    foreach ($placeTypeMap as $keyword => $type) {
        if (mb_stripos($name, $keyword) !== false) {
            return $type;
        }
    }
    
    // Google API 타입 기반 매칭
    if (isset($place['types'])) {
        foreach ($place['types'] as $type) {
            if (isset($placeTypeMap[$type])) {
                return $placeTypeMap[$type];
            }
        }
    }
    
    return '관광명소';
}

// GPT를 사용한 태그 생성 요청 준비
function prepareGptRequests($places, $placeTypes, $tags, $apiKey) {
    $requests = [];
    foreach ($places as $index => $place) {
        $prompt = "다음 장소의 특성을 분석하여 가장 적절한 태그 4개를 선택해주세요.\n\n" .
                 "장소: {$place['name']}\n" .
                 "유형: {$placeTypes[$index]}\n\n" .
                 "사용 가능한 태그:\n" . implode(", ", $tags) . "\n\n" .
                 "응답은 쉼표로 구분된 태그 4개만 작성해주세요.";

        $requests[] = [
            'url' => 'https://api.openai.com/v1/chat/completions',
            'headers' => [
                'Content-Type: application/json',
                'Authorization: Bearer ' . $apiKey
            ],
            'post_data' => json_encode([
                'model' => 'gpt-4o-mini',
                'messages' => [
                    ['role' => 'system', 'content' => '당신은 주어진 장소의 특성을 분석하여 가장 적절한 태그를 선택하는 전문가입니다.'],
                    ['role' => 'user', 'content' => $prompt]
                ],
                'temperature' => 0.7,
                'max_tokens' => 100
            ])
        ];
    }
    return $requests;
}

// 메인 처리 로직
$responseData = [
    "place_names" => [],
    "place_addresses" => [],
    "latitudes" => [],
    "longitudes" => [],
    "place_type" => [],
    "tags" => []
];

if (!empty($selectedLocations)) {
    foreach ($selectedLocations as $location) {
        writeLog("Processing location: " . $location);

        $placesToProcess = [];
        $placeTypes = [];

        // 각 타입별로 페이지네이션 적용하여 장소 가져오기
        foreach ($typeList as $type) {
            $places = fetchPlacesWithPagination($location, $type, $googleApiKey, $maxResultsPerType);

            foreach ($places as $place) {
                if (!in_array($place["name"], $responseData["place_names"])) {
                    $placesToProcess[] = $place;
                    $placeTypes[] = getDetailedPlaceType($place, $placeTypeMap);
                }
            }
        }

        if (empty($placesToProcess)) {
            continue;
        }

        // Geocoding 요청 준비
        $geocodeRequests = array_map(function($place) use ($googleApiKey) {
            return [
                'url' => "https://maps.googleapis.com/maps/api/geocode/json?" .
                        "address=" . urlencode($place["formatted_address"]) .
                        "&key=" . $googleApiKey
            ];
        }, $placesToProcess);

        // GPT 태그 생성 요청 준비
        $gptRequests = prepareGptRequests($placesToProcess, $placeTypes, $tags, $apiKey);

        // 병렬로 Geocoding과 GPT 요청 실행
        $geocodeResponses = makeParallelRequests($geocodeRequests);
        $tagResponses = makeParallelRequests($gptRequests);

        // 결과 처리
        foreach ($placesToProcess as $index => $place) {
            if (isset($geocodeResponses[$index]["results"][0]["geometry"]["location"])) {
                $responseData["place_names"][] = $place["name"];
                $responseData["place_addresses"][] = $place["formatted_address"];
                $responseData["latitudes"][] = $geocodeResponses[$index]["results"][0]["geometry"]["location"]["lat"];
                $responseData["longitudes"][] = $geocodeResponses[$index]["results"][0]["geometry"]["location"]["lng"];
                $responseData["place_type"][] = $placeTypes[$index];

                // GPT 응답에서 태그 추출
                $suggestedTags = [];
                if (isset($tagResponses[$index]["choices"][0]["message"]["content"])) {
                    $suggestedTags = array_slice(
                        array_map('trim', 
                        explode(',', $tagResponses[$index]["choices"][0]["message"]["content"])
                        ), 0, 4
                    );
                }
                $responseData["tags"][] = $suggestedTags;

                writeLog("Added place: {$place["name"]} with type: {$placeTypes[$index]} and tags: " . implode(", ", $suggestedTags));
            }
        }
    }
}

writeLog("Total places processed: " . count($responseData["place_names"]));
$conn->close();

// auto_route.php로 데이터 전송
$autoRouteEndpoint = "http://YOUR_DB_HOST/auto_route.php";  // 실제 도메인으로 변경 필요

$ch = curl_init($autoRouteEndpoint);
curl_setopt_array($ch, [
    CURLOPT_POST => 1,
    CURLOPT_POSTFIELDS => json_encode($responseData, JSON_UNESCAPED_UNICODE),
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json; charset=UTF-8'
    ],
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT => 1,
    CURLOPT_NOSIGNAL => 1
]);

$forwardResponse = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

writeLog("Data forwarded to auto_route.php - HTTP Status: " . $httpCode);

// 클라이언트에게 응답
echo json_encode($responseData, JSON_UNESCAPED_UNICODE);

?>