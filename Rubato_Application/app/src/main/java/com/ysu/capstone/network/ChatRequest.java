package com.ysu.capstone.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("prompt")
    @Expose
    private String prompt;

    @SerializedName("user_name")
    @Expose
    private String user_name;

    @SerializedName("user_email")
    @Expose
    private String user_email;

    @SerializedName("session_id")
    @Expose
    private String session_id;

    public ChatRequest(String prompt, String user_name, String user_email, String session_id) {
        this.prompt = prompt;
        this.user_name = user_name;
        this.user_email = user_email;
        this.session_id = session_id;
    }

    // Getters and Setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getUser_name() { return user_name; }
    public void setUser_name(String user_name) { this.user_name = user_name; }

    public String getUser_email() { return user_email; }
    public void setUser_email(String user_email) { this.user_email = user_email; }

    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "prompt='" + prompt + '\'' +
                ", user_name='" + user_name + '\'' +
                ", user_email='" + user_email + '\'' +
                ", session_id='" + session_id + '\'' +
                '}';
    }
}