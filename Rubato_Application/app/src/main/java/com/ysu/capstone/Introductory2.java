package com.ysu.capstone;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class Introductory2 extends AppCompatActivity {

    ImageView miniLogo, app_name, bar;
    LottieAnimationView lottie;
    Button letgo;
    TextView welcomeMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introductory2);

        letgo = findViewById(R.id.letgo);
        miniLogo = findViewById(R.id.miniLogo);
        app_name = findViewById(R.id.appName);
        bar = findViewById(R.id.bar);
        lottie = findViewById(R.id.lottie);
        welcomeMessage = findViewById(R.id.welcomeMessage);

        // Intent로 전달된 사용자 정보 받기
        Intent intent = getIntent();
        String user_name = intent.getStringExtra("user_name");
        String user_email = intent.getStringExtra("user_email");

        // SharedPreferences에 user_name 저장
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_name", user_name);
        editor.putString("user_email", user_email);
        editor.apply();

        // 환영 메시지 설정


        // 애니메이션 설정
        bar.animate().translationY(-400).setDuration(700).setStartDelay(2800);
        app_name.animate().translationY(-400).setDuration(700).setStartDelay(2800);
        miniLogo.animate().translationY(-400).setDuration(700).setStartDelay(2800);
        lottie.animate().translationY(3000).setDuration(700).setStartDelay(2500);

        // 환영 메시지 애니메이션 설정
        welcomeMessage.setAlpha(0f);
        welcomeMessage.setVisibility(View.VISIBLE);

        welcomeMessage.postDelayed(() -> {
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(welcomeMessage, "alpha", 0f, 1f);
            fadeIn.setDuration(1000);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            fadeIn.start();
        }, 3500); // 모든 아이콘 애니메이션 이후에 시작

        letgo.setOnClickListener(v -> {
            Intent intentMain = new Intent(Introductory2.this, MainActivity.class);
            startActivity(intentMain);
            finish(); // Introductory2 액티비티를 종료
        });
    }
}
