<?php 
header("Content-Type: application/json; charset=UTF-8");

//오류메시지
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$apiKey = getenv('OPENAI_API_KEY');

//api키가 없을 경우
if (!$apiKey) {
    error_log("API key not found");
    die(json_encode(array("status" => "error", "message" => "API key not found")));
}

// json을 raw로 읽어오기
$json = file_get_contents('php://input');

// 받은 json 로그로 확인
if (!$json) {
    error_log("No JSON input received");
    die(json_encode(array("status" => "error", "message" => "No input data provided.")));
}

// JSON 디코딩
$data = json_decode($json, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    error_log("JSON decode error: " . json_last_error_msg());
    die(json_encode(array("status" => "error", "message" => "Invalid JSON data.")));
}

// 입력 데이터 로그로 확인
error_log("Received data: " . json_encode($data));


// 'location_name' 값 확인
$location_name = $data['location_name'] ?? '';
if (empty($location_name)) {
    error_log("No location_name provided");
    die(json_encode(array("status" => "error", "message" => "No location_name provided.")));
}

// ChatGPT API 요청
$data = [
    "model" => "gpt-4o-mini",
    "stream" => False,
    "messages" => [
        [
            "role" => "system",
            "content" => "You are a realistic and friendly travel guide. If the user’s selected location is not a well-known tourist spot, suggest visiting a nearby area as well. For example, 'OO is less known, but why not also visit XX nearby?' Always respond in Korean, keeping explanations within 70 characters. When suggesting alternatives, recommend nearby regions without listing specific places. Focus on constructive and friendly suggestions to encourage exploration."




        ],
        [
            "role" => "user", 
            "content" => "지역 이름: " . $location_name . ", 날짜: " . date("Y년 m월 d일")
        ]
    ]
];

$ch = curl_init('https://api.openai.com/v1/chat/completions');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $apiKey
]);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));

curl_setopt($ch, CURLOPT_WRITEFUNCTION, function($ch, $data) {
    echo $data;
    ob_flush();
    flush();
    return strlen($data);
});

$result = curl_exec($ch);

// cURL 실행 결과 확인
if ($result === false) {
    error_log("cURL error: " . curl_error($ch));
}

curl_close($ch);

echo $result;
?>