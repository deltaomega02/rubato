package com.ysu.capstone.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("user_email")
    @Expose
    private String user_email;

    @SerializedName("user_passwd")
    @Expose
    private String user_passwd;

    // 생성자 및 Getter, Setter
    public LoginRequest(String user_email, String user_passwd) {
        this.user_email = user_email;
        this.user_passwd = user_passwd;
    }

    @Override
    public String toString() {
        return "ApiRequest{" +
                ", user_email='" + user_email + '\'' +
                ", user_passwd='" + user_passwd + '\'' +
                '}';
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_passwd() {
        return user_passwd;
    }

    public void setUser_passwd(String user_passwd) {
        this.user_passwd = user_passwd;
    }
}