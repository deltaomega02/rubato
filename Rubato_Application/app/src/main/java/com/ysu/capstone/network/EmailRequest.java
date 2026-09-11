package com.ysu.capstone.network;

public class EmailRequest {
    private String user_email;

    // 생성자
    public EmailRequest(String user_email) {
        this.user_email = user_email;
    }

    // getter 메서드
    public String getUser_email() {
        return user_email;
    }

    // setter 메서드
    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }
}
