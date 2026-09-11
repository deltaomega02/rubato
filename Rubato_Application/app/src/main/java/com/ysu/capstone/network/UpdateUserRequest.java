package com.ysu.capstone.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UpdateUserRequest {
    @SerializedName("user_name")
    @Expose
    private String user_name;

    @SerializedName("user_email")
    @Expose
    private String user_email;

    // 생성자 및 Getter, Setter
    public UpdateUserRequest(String user_name, String user_email) {
        this.user_name = user_name;
        this.user_email = user_email;
    }

    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "user_name='" + user_name + '\'' +
                ", user_email='" + user_email + '\'' +
                '}';
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
