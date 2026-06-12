package com.ysu.capstone.chat;

import android.view.View;

public class Message {
    private String text;  // 메시지 내용
    private String sender;  // "user" 또는 "bot"
    private String type;   // "text" 또는 "schedule_card"
    private View scheduleView;  // 스케줄 카드뷰 저장

    public Message(String text, String sender) {
        this.text = text;
        this.sender = sender;
        this.type = "text";  // 기본 타입은 text
    }

    public Message(String text, String sender, String type) {
        this.text = text;
        this.sender = sender;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setMessage(String text) {
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public String getType() {
        return type;
    }

    public View getScheduleView() {
        return scheduleView;
    }

    public void setScheduleView(View scheduleView) {
        this.scheduleView = scheduleView;
        this.type = "schedule_card";
    }
}