package com.ysu.capstone.network;

public class ChatResponse {
    private String status;  // 성공 또는 실패 여부
    private String message;  // 응답 메시지
    private String chat_response;  // ChatGPT 응답

    // Getter 및 Setter
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChat_response() {
        return chat_response;
    }

    public void setChat_response(String chat_response) {
        this.chat_response = chat_response;
    }
}