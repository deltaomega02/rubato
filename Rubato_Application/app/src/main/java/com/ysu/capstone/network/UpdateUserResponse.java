package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;

public class UpdateUserResponse {
    @SerializedName("status")
    private String status;  // 성공 또는 실패 여부

    @SerializedName("message")
    private String message;  // 응답 메시지

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

    @Override
    public String toString() {
        return "UpdateUserResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
