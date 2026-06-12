package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    private String status;  // 성공 또는 실패 여부

    @SerializedName("message")
    private String message;  // 응답 메시지

    @SerializedName("user_name")
    private String user_name;  // 사용자 이름

    @SerializedName("user_email")
    private String user_email;  // 사용자 이메일

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

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }
}