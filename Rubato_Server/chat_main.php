<?php
header("Content-Type: application/json; charset=UTF-8");
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$totalStartTime = microtime(true);
$apiKey = getenv('OPENAI_API_KEY');
$geminiApiKey = 'YOUR_GOOGLE_API_KEY';

if (!$apiKey) {
    error_log("OpenAI API key not found");
    die(json_encode(array("status" => "error", "message" => "OpenAI API key not found")));
}

// JSON 읽기 및 디코딩
$json = file_get_contents('php://input');
$data = json_decode($json, true);

if (json_last_error() !== JSON_ERROR_NONE) {
    error_log("JSON decode error: " . json_last_error_msg());
    die(json_encode(array("status" => "error", "message" => "Invalid JSON data.")));
}

$promptData = json_decode($data['prompt'], true);

if (!$promptData) {
    error_log("Prompt data decode error: " . json_last_error_msg());
    die(json_encode(array("status" => "error", "message" => "Invalid prompt data.")));
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
    die(json_encode(array("status" => "error", "message" => "Database connection failed")));
}

// 세션 상태 확인
function checkSessionState($conn, $sessionId) {
    $stmt = $conn->prepare("
        SELECT session_type 
        FROM session_state 
        WHERE session_id = :session_id
    ");
    $stmt->execute(['session_id' => $sessionId]);
    return $stmt->fetch(PDO::FETCH_ASSOC);
}

// 세션 상태 설정
function setSessionState($conn, $sessionId, $type) {
    $stmt = $conn->prepare("
        INSERT INTO session_state (session_id, session_type, created_at)
        VALUES (:session_id, :session_type, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE 
        session_type = :session_type,
        updated_at = CURRENT_TIMESTAMP
    ");
    $stmt->execute([
        'session_id' => $sessionId,
        'session_type' => $type
    ]);
}

// 채팅 기록 삭제
function clearChatHistory($conn, $sessionId) {
    error_log("Clearing chat history for session: " . $sessionId);
    $stmt = $conn->prepare("
        DELETE FROM chat_history 
        WHERE session_id = :session_id
    ");
    $stmt->execute(['session_id' => $sessionId]);
    error_log("Chat history cleared for session: " . $sessionId);
}

// 현재 세션 상태 확인
$sessionState = checkSessionState($conn, $data['session_id']);
error_log("Current session state: " . ($sessionState ? $sessionState['session_type'] : 'new session'));

// 의도 파악을 위한 GPT 호출
$intentCheckMessages = array();
$intentCheckMessages[] = array(
    "role" => "system",
    "content" => "사용자의 입력이 일정 요청인지 정보 검색 요청인지 판단해주세요.

1. 일정 요청 - 여행 일정을 짜달라는 요청 (예: '일정 좀 짜줘', '코스 추천해줘', '여행 계획 세워줘')
2. 정보 검색 - 특정 장소나 정보에 대해 묻는 요청 (예: '이용요금이 얼마야?', '영업시간 알려줘', '맛집 추천해줘')

반드시 다음 JSON 형식으로만 응답하세요:
{\"type\": \"schedule\"} 또는 {\"type\": \"information\"}"
);
$intentCheckMessages[] = array("role" => "user", "content" => $promptData['prompt']);

// 이미 schedule 세션이 있는 경우 무조건 schedule로 라우팅
if ($sessionState && $sessionState['session_type'] === 'schedule') {
    error_log("Continuing existing schedule session: " . $data['session_id']);
    include 'chat_route.php';
    die();
}

// 새로운 요청에 대한 의도 파악
$intentStartTime = microtime(true);
$intentResponse = callGPTForIntent($intentCheckMessages, $apiKey);
$intentData = json_decode($intentResponse, true);
error_log("Intent detection result: " . $intentData['type']);

// 라우팅 및 세션 상태 설정
if ($intentData['type'] === 'schedule') {
    // 새로운 schedule 세션 시작 시 이전 채팅 기록 삭제
    if (!$sessionState || $sessionState['session_type'] !== 'schedule') {
        error_log("Starting new schedule session, clearing previous chat history");
        clearChatHistory($conn, $data['session_id']);
    }
    
    // schedule 타입으로 세션 상태 설정
    setSessionState($conn, $data['session_id'], 'schedule');
    error_log("Set session state to schedule: " . $data['session_id']);
    include 'chat_route.php';
} else {
    // information 타입으로 세션 상태 설정
    setSessionState($conn, $data['session_id'], 'information');
    error_log("Set session state to information: " . $data['session_id']);
    include 'chat_search.php';
}

function callGPTForIntent($messages, $apiKey) {
    $ch = curl_init('https://api.openai.com/v1/chat/completions');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey
    ));
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(array(
        "model" => "gpt-4o-mini",
        "messages" => $messages
    )));
    
    $response = curl_exec($ch);
    curl_close($ch);
    
    $result = json_decode($response, true);
    return $result['choices'][0]['message']['content'];
}
$totalEndTime = microtime(true);
error_log(sprintf("Total Execution Time: %.2f seconds", $totalEndTime - $totalStartTime));
?>