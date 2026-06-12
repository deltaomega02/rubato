package com.ysu.capstone.chat;

import android.content.Context;
import android.content.SharedPreferences;

public class ClearChatHistory {private static final String CHAT_HISTORY_PREF = "ChatHistory";

    // 채팅 로그 초기화 메서드
    public static void clearChatHistory(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CHAT_HISTORY_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}