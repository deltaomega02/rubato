package com.ysu.capstone;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;
import android.graphics.Color;
import android.text.method.PasswordTransformationMethod;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.android.material.snackbar.Snackbar;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.EmailRequest;
import com.ysu.capstone.network.RegisterRequest;
import com.ysu.capstone.network.RegisterResponse;
import com.ysu.capstone.network.RetrofitClient;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUp extends Dialog {

    private EditText editTextId, verificationCode, editName, pwdText, checkPwd;
    private Button signupButton;
    private ImageView closeButton;
    private ConstraintLayout signupLayout;
    private androidx.cardview.widget.CardView cardView; // CardView 추가 선언

    private String user_name, user_email, pwdtext, user_passwd, verificationCodeSent, userInputCode;
    private int currentStep = 1;  // 회원가입 진행 단계 추적

    public SignUp(Context context) {
        super(context);
        setContentView(R.layout.ic_signup);

        // 배경을 투명하게 설정
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        signupLayout = findViewById(R.id.signup_layout);
        editTextId = findViewById(R.id.editTextId);
        verificationCode = findViewById(R.id.verification_code);
        editName = findViewById(R.id.editname);
        pwdText = findViewById(R.id.pwdtext);
        checkPwd = findViewById(R.id.checkpwd);
        signupButton = findViewById(R.id.signupbutton);
        closeButton = findViewById(R.id.closeButton);
        cardView = findViewById(R.id.card_signup); // XML에서 CardView 초기화

        // CardView 배경 투명하게 설정
        cardView.setCardBackgroundColor(Color.TRANSPARENT);

        // 비밀번호 점으로 표시
        pwdText.setTransformationMethod(new DotPasswordTransformationMethod());
        checkPwd.setTransformationMethod(new DotPasswordTransformationMethod());

        // 초기 숨김 설정
        verificationCode.setVisibility(View.GONE);
        editName.setVisibility(View.GONE);
        pwdText.setVisibility(View.GONE);
        checkPwd.setVisibility(View.GONE);

        closeButton.setOnClickListener(v -> dismiss());

        signupButton.setOnClickListener(v -> handleButtonClick());
    }

    private void handleButtonClick() {
        switch (currentStep) {
            case 1:
                handleEmailVerification();
                break;
            case 2:
                handleVerificationCode();
                break;
            case 3:
                handleNameInput();
                break;
            case 4:
                ppassword();
                break;
            case 5:
                handlePasswordConfirmation();
                break;

        }
    }

    private void showSnackbar(String message, boolean isPositive) {
        View parentView = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(getContext(), R.color.skyblue))
                .setTextColor(Color.WHITE);

        // 부정적인 경우 `shake` 애니메이션 추가
        if (!isPositive) {
            Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            parentView.startAnimation(shake);
        }

        // 스낵바 위치 설정
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.bottomMargin = 0;
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }

    private void handleEmailVerification() {
        user_email = editTextId.getText().toString();
        if (isValidEmail(user_email)) {
            editTextId.setError(null); // 이메일 형식이 유효한 경우 오류 제거

            // 이메일 중복 확인
            checkEmailDuplicate(user_email);
        } else {
            showSnackbar("올바른 이메일 형식이 아닙니다.", false);
        }
    }

    private void checkEmailDuplicate(String email) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        // 새로운 코드로 대체
        Map<String, String> request = new HashMap<>();
        request.put("user_email", email);
        request.put("action", "check");

        apiService.checkEmail(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        String message = jsonResponse.getString("message");

                        if (status.equals("error") && message.equals("email_exists")) {
                            showSnackbar("이미 존재하는 이메일입니다.", false);
                        } else if (status.equals("success") && message.equals("verification_sent")) {
                            verificationCodeSent = jsonResponse.getString("verification_code");
                            showSnackbar("이메일을 전송했습니다.", true);
                            editTextId.setEnabled(false);
                            animateVisibilityAndPosition(verificationCode);
                            updateConstraints(R.id.editTextId, R.id.verification_code);
                            signupButton.setText("인증번호 확인");
                            currentStep = 2;
                        }
                    } else {
                        showSnackbar("이미 존재하는 이메일입니다", false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showSnackbar("응답 처리 오류", false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showSnackbar("네트워크 오류", false);
            }
        });
    }
    private void handleVerificationCode() {
        userInputCode = verificationCode.getText().toString();
        if (userInputCode.isEmpty()) {
            showSnackbar("인증번호를 입력해주세요.", false);
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("user_email", user_email);
        request.put("action", "verify");
        request.put("verification_code", userInputCode);
        request.put("sent_code", verificationCodeSent);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.verifyCode(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getString("status").equals("success")) {
                            showSnackbar("인증번호가 확인되었습니다.", true);
                            verificationCode.setEnabled(false);
                            animateVisibilityOnly(signupButton);
                            animateVisibilityAndPosition(editName);
                            updateConstraints(R.id.verification_code, R.id.editname);
                            signupButton.setText("다음");
                            currentStep = 3;
                        } else {
                            showSnackbar("인증번호가 일치하지 않습니다.", false);
                        }
                    } else {
                        showSnackbar("인증번호가 일치하지 않습니다.", false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showSnackbar("응답 처리 오류", false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showSnackbar("네트워크 오류", false);
            }
        });
    }

    private void handleNameInput() {
        user_name = editName.getText().toString();
        if (user_name.isEmpty()) {
            //showSnackbar("이름을 입력해주세요.", false);
        } else {
            //showSnackbar("이름이 등록되었습니다.", true);
            editName.setEnabled(false);

            animateVisibilityOnly(signupButton);
            animateVisibilityAndPosition(pwdText);
            updateConstraints(R.id.editname, R.id.pwdtext);

            signupButton.setText("다음");
            currentStep = 4;
        }
    }

    private void ppassword() {
        pwdtext = pwdText.getText().toString();
        if (pwdtext.isEmpty()) {
            // showSnackbar("비밀번호를 입력해주세요.", false);
        } else {
            // showSnackbar("비밀번호가 입력되었습니다.", true);
            pwdText.setEnabled(false);

            animateVisibilityOnly(signupButton);
            animateVisibilityAndPosition(checkPwd);
            updateConstraints(R.id.pwdtext, R.id.checkpwd);

            signupButton.setText("비밀번호 확인");
            currentStep = 5;
        }
    }

    private void handlePasswordConfirmation() {
        if (!checkPwd.getText().toString().equals(pwdText.getText().toString())) {
            showSnackbar("비밀번호가 일치하지 않습니다.", false);
        } else {
            user_passwd = checkPwd.getText().toString();

            // 서버에 사용자 등록 요청
            registerUser(user_name, user_email, user_passwd);
        }
    }


    private void registerUser(String userName, String userEmail, String userPassword) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        RegisterRequest registerRequest = new RegisterRequest(userName, userEmail, userPassword);

        apiService.registerUser(registerRequest).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    if ("success".equals(registerResponse.getStatus())) {
                        // 성공적으로 회원가입이 완료된 경우
                        SharedPreferences sharedPreferences = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("user_name", userName);
                        editor.putString("user_email", userEmail);
                        editor.apply();

                        // 메인 화면으로 이동
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        getContext().startActivity(intent);
                        dismiss(); // 다이얼로그 종료
                    } else {
                        showSnackbar("회원가입에 실패했습니다.", false);
                    }
                } else {
                    showSnackbar("서버 응답 실패: " + response.message(), false);
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                showSnackbar("서버 요청 실패: " + t.getMessage(), false);
            }
        });
    }

    // 비밀번호 점으로 표시하기 위한 클래스
    public class DotPasswordTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new PasswordCharSequence(source);
        }

        private class PasswordCharSequence implements CharSequence {
            private final CharSequence mSource;

            public PasswordCharSequence(CharSequence source) {
                mSource = source;
            }

            @Override
            public char charAt(int index) {
                return '•'; // 비밀번호 대신 동그라미 표시
            }

            @Override
            public int length() {
                return mSource.length();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return mSource.subSequence(start, end);
            }
        }
    }

    // 이메일 형식이 올바른지 확인하는 메서드
    private boolean isValidEmail(String email) {
        email = email.trim();
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }


    private void animateVisibilityAndPosition(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.setVisibility(View.VISIBLE);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(300);

        ObjectAnimator moveDown = ObjectAnimator.ofFloat(view, "translationY", -70, 0);
        moveDown.setDuration(700);

        fadeIn.start();
        moveDown.start();
    }

    private void animateVisibilityOnly(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.setVisibility(View.VISIBLE);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();
    }

    private void updateConstraints(int startViewId, int targetViewId) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(signupLayout);

        constraintSet.connect(targetViewId, ConstraintSet.TOP, startViewId, ConstraintSet.BOTTOM, 70);
        TransitionManager.beginDelayedTransition(signupLayout);
        constraintSet.applyTo(signupLayout);
    }

}

