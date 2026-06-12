package com.ysu.capstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.ysu.capstone.network.LoginRequest;
import com.ysu.capstone.network.LoginResponse;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Introductory extends AppCompatActivity {
    LinearLayout login_layout;
    EditText userName, password;
    CheckBox keepLogin;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introductory);

        // UI 요소 초기화
        login_layout = findViewById(R.id.loginLayout);
        userName = findViewById(R.id.userName);
        password = findViewById(R.id.password);
        keepLogin = findViewById(R.id.keepLogin);
        loginButton = findViewById(R.id.loginButton);
        TextView signup = findViewById(R.id.signup);

        // 로그인 상태 확인
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);
        if (isLoggedIn) {
            navigateToMainActivity();
            return;
        }

        // 로그아웃 메시지 처리
        handleLogoutMessage();

        // 로그인 버튼 클릭 처리
        loginButton.setOnClickListener(v -> {
            String userEmail = userName.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                showSnackbar("아이디와 비밀번호를 입력하세요.");
                return;
            }

            loginUser(userEmail, userPassword);
        });

        // 회원가입 다이얼로그 호출
        signup.setOnClickListener(v -> {
            SignUp signUpDialog = new SignUp(this);
            signUpDialog.show();
        });
    }

    private void loginUser(String userEmail, String userPasswd) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(userEmail, userPasswd);

        apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if ("success".equals(loginResponse.getStatus())) {
                        saveLoginDetails(loginResponse);
                        navigateToMainActivity();
                    } else {
                        showSnackbar(loginResponse.getMessage());
                    }
                } else {
                    showSnackbar("서버 응답 실패: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showSnackbar("네트워크 오류: " + t.getMessage());
            }
        });
    }

    private void saveLoginDetails(LoginResponse loginResponse) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor2 = sharedPreferences.edit();

        editor2.putString("user_name", loginResponse.getUser_name());
        editor2.putString("user_email", loginResponse.getUser_email());
        editor2.putBoolean("is_logged_in", keepLogin.isChecked());

        // 사용자별 첫 로그인 플래그를 항상 true로 설정
        editor2.putBoolean("is_first_run_" + loginResponse.getUser_name(), true);

        editor2.apply();
    }

    private void handleLogoutMessage() {
        String snackbarMessage = getIntent().getStringExtra("snackbar_message");
        if (snackbarMessage != null && !snackbarMessage.isEmpty()) {
            showSnackbar(snackbarMessage);

            // 로그아웃 메시지를 저장
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("logout_message", snackbarMessage);
            editor.apply();
        }

        // 저장된 로그아웃 메시지 확인 및 삭제
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String logoutMessage = preferences.getString("logout_message", null);
        if (logoutMessage != null) {
            showSnackbar(logoutMessage);
            preferences.edit().remove("logout_message").apply();
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(Introductory.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.skyblue))
                .setTextColor(Color.BLACK);
        snackbar.show();
    }
}
