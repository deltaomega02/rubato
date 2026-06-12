package com.ysu.capstone.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RouteDetailRequest {
    @SerializedName("email")
    @Expose
    private String email;  // 사용자 이메일

    // 생성자
    public RouteDetailRequest(String email) {
        this.email = email;
    }

    // Getter 및 Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "RouteDetailRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
