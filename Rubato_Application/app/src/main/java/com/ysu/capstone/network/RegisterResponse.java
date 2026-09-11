package com.ysu.capstone.network;

public class RegisterResponse {

    private String status;   // 요청 상태 (예: "success" 또는 "error")
    private String message;  // 응답 메시지 (예: "User registered successfully")

    // Getter와 Setter (필요 시 추가)
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
}

