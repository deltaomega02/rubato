<?php
header('Content-Type: text/html; charset=UTF-8');
ini_set('default_charset', 'UTF-8');
ini_set('log_errors', 1);
ini_set('error_log', '/var/log/apache2/auto_route.log');

$rawData = file_get_contents("php://input");
error_log("Received raw data: " . $rawData);

$apiKey = getenv('OPENAI_API_KEY');
//$placeApiKey = "YOUR_GOOGLE_API_KEY";
$placeApiKey = "YOUR_GOOGLE_API_KEY";

function writeLog($message, $type = 'INFO') {
    $logMessage = date('Y-m-d H:i:s') . " [$type] " . mb_convert_encoding($message, 'UTF-8', 'auto') . "\n";
    error_log($logMessage);
}

function getPlaceDetailsBatch($places) {
    global $placeApiKey;
    $results = [];
    $batchSize = 10;
    $maxConcurrentBatches = 8;
    
    // 80개의 관광지 정보를 8개의 배치로 나누어 동시 호출
    $mh = curl_multi_init();
    $channels = [];
    
    for ($i = 0; $i < $maxConcurrentBatches; $i++) {
        for ($j = 0; $j < $batchSize; $j++) {
            $placeIndex = $i * $batchSize + $j;
            $placeUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json"
                . "?query=" . urlencode($places[$placeIndex % count($places)])
                . "&key=" . $placeApiKey
                . "&language=ko";

            $ch = curl_init($placeUrl);
            curl_setopt_array($ch, [
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_SSL_VERIFYPEER => false
            ]);

            curl_multi_add_handle($mh, $ch);
            $channels[] = $ch;
        }
    }

    $running = null;
    do {
        curl_multi_exec($mh, $running);
        curl_multi_select($mh);
    } while ($running > 0);
    
    foreach ($channels as $ch) {
        $response = curl_multi_getcontent($ch);
        $result = json_decode($response, true);

        if (isset($result['results'][0])) {
            $place = $result['results'][0];
            $results[] = [
                'name' => $place['name'],
                'address' => $place['formatted_address'],
                'latitude' => (string)$place['geometry']['location']['lat'],
                'longitude' => (string)$place['geometry']['location']['lng']
            ];
        }

        curl_multi_remove_handle($mh, $ch);
        curl_close($ch);
    }
    
    curl_multi_close($mh);
    return array_unique($results, SORT_REGULAR);
}

function streamResponse($messages) {
    global $apiKey;

    $requestData = [
        "model" => "gpt-4o",
        "messages" => $messages,
        "temperature" => 0.7
    ];

    writeLog("Sending request to GPT API: " . json_encode($requestData));

    $ch = curl_init('https://api.openai.com/v1/chat/completions');
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/json',
            'Authorization: Bearer ' . $apiKey
        ],
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => json_encode($requestData)
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    writeLog("GPT API HTTP Code: " . $httpCode);
    writeLog("GPT API Raw Response: " . $response);

    if ($response === false) {
        writeLog("Curl error: " . curl_error($ch), "ERROR");
        echo json_encode([
            "status" => "error",
            "message" => "일정 생성 중 오류가 발생했습니다."
        ]);
        curl_close($ch);
        return;
    }
    curl_close($ch);

    $result = json_decode($response, true);
    if (!isset($result['choices'][0]['message']['content'])) {
        writeLog("Invalid GPT response structure", "ERROR");
        echo json_encode([
            "status" => "error",
            "message" => "일정 생성 중 오류가 발생했습니다."
        ]);
        return;
    }

    $content = $result['choices'][0]['message']['content'];
    writeLog("GPT Content Response: " . $content);

    try {
        $decodedContent = json_decode($content, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("Invalid JSON format: " . json_last_error_msg());
        }

        if (!isset($decodedContent['schedule'])) {
            throw new Exception("Missing schedule field");
        }

        $response = [
            "status" => "success",
            "response" => "여행 일정이 성공적으로 생성되었습니다.",
            "schedule" => $decodedContent['schedule']
        ];

        echo json_encode($response, JSON_UNESCAPED_UNICODE);
        return;

    } catch (Exception $e) {
        writeLog("Error processing GPT response: " . $e->getMessage(), "ERROR");
        echo json_encode([
            "status" => "error",
            "message" => "일정 생성에 실패했습니다. 다시 시도해주세요."
        ]);
    }
}

$data = json_decode($rawData, true);

if ($data) {
    writeLog("=== 클라이언트 요청 데이터 ===");
    
    if (!isset($data['selected_locations']) || 
        !isset($data['selected_tags']) || !is_array($data['selected_tags']) ||
        !isset($data['num_days']) || !is_numeric($data['num_days'])) {
        writeLog("Invalid request data", "ERROR");
        echo json_encode([
            'status' => 'error',
            'message' => '요청 데이터가 유효하지 않습니다.'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if (!is_array($data['selected_locations'])) {
        $data['selected_locations'] = [$data['selected_locations']];
    }

    writeLog("선택된 지역: " . implode(", ", $data['selected_locations']));
    writeLog("선택된 태그: " . implode(", ", $data['selected_tags']));
    writeLog("여행 일수: " . $data['num_days'] . "일");

    $places = getPlaceDetailsBatch($data['selected_locations']);
    writeLog("Place API Results: " . json_encode($places));

    $systemMessage = "당신은 여행 일정을 계획하는 전문 여행 플래너입니다. 다음 조건에 맞는 여행 일정을 만들어주세요:\n\n";
    $systemMessage .= "1. 여행 기본 정보\n";
    $systemMessage .= "- 여행 기간: {$data['num_days']}일\n";
    $systemMessage .= "- 여행 지역: " . implode(", ", $data['selected_locations']) . "\n";
    $systemMessage .= "- 여행 스타일: " . implode(", ", $data['selected_tags']) . "\n\n";
    $systemMessage .= "2. 필수 포함 사항\n";
    $systemMessage .= "- 하루 최소 3곳 이상의 관광지\n";
    $systemMessage .= "- 매일 아침/점심/저녁 식사 장소\n";
    $systemMessage .= "- 선택된 태그에 맞는 장소 구성\n";
    $systemMessage .= "- 현실적인 이동 동선\n\n";
    $systemMessage .= "3. 장소 선정 시 주의사항\n";
    $systemMessage .= "- 실제 존재하는 유명 관광지나 장소 위주로 선정\n";
    $systemMessage .= "- 정확한 장소명 사용\n";
    $systemMessage .= "- 식당은 유명하고 검색 가능한 곳으로 선정\n\n";
    $systemMessage .= "\n\n다음 JSON 형식으로 응답해주세요:\n";
    $systemMessage .= "{\n  \"schedule\": {\n    \"1\": [\n      {\"name\":\"장소명\",\"address\":\"주소\",\"latitude\":\"위도\",\"longitude\":\"경도\"}\n    ]\n  }\n}\n";

    $messages = [
        ["role" => "system", "content" => $systemMessage],
        ["role" => "system", "content" => "Please always answer in Korean."]
    ];

    streamResponse($messages);
} else {
    writeLog("데이터 수신 실패", "ERROR");
    echo json_encode([
        'status' => 'error',
        'message' => '데이터 수신 실패'
    ], JSON_UNESCAPED_UNICODE);
}
?>