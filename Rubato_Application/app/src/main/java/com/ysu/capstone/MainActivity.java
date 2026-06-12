package com.ysu.capstone;
import static com.google.android.material.internal.ViewUtils.showKeyboard;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.drawable.ColorDrawable;

// Android View & Widget 관련
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

// Android Animation 관련
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.AnimatorListenerAdapter;

// Android Graphics & UI 효과
import android.graphics.BlurMaskFilter;
import android.graphics.Paint;

// Android Dialog & Alert 관련
import android.app.AlertDialog;
import android.content.DialogInterface;

// Android 유틸리티
import android.util.Log;
import android.widget.Toast;

// AndroidX 컴포넌트
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

// Google Material Design 컴포넌트
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;

// 프로젝트 커스텀 컴포넌트
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ysu.capstone.chat.ClearChatHistory;
import com.ysu.capstone.decorators.RecommendSliderAdapter;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RouteDetailRequest;
import com.ysu.capstone.network.RouteDetailResponse;
import com.ysu.capstone.network.UpdateUserRequest;
import com.ysu.capstone.network.UpdateUserResponse;
import com.ysu.capstone.network.RetrofitClient;

// Java 유틸리티
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private DrawerLayout drawerLayout;
    private View myPrivacyLayout;
    private View myChallengeLayout;
    private View myReviewLayout;
    private View containerButton1;
    private View containerButton2;
    private View containerButton3;

    private FrameLayout containerButton8_1;
    private FrameLayout containerButton8_2;
    private FrameLayout containerButton8_3;
    private FrameLayout containerButton8_4;
    private FrameLayout containerButton8_5;
    private FrameLayout containerButton8_6;
    private FrameLayout containerButton8_7;
    private FrameLayout containerButton8_8;

    private View lastSelectedLayout;

    private View activeIndicator;
    private List<Integer> imageList;
    private int totalPages;
    private RecommendSliderAdapter adapter;
    private ImageView imageView;
    private Handler slideHandler = new Handler(Looper.getMainLooper());
    private static final int SLIDE_DELAY = 5500;


    private View bottomSheet;
    private View recommendedPlaces;
    private View search_icon;
    private View options_area;

    private ImageView imagePopular, imagePopular1, imagePopular2, imagePopular3, Imageview;

    private TextView intendedTravelDday;
    private ImageView ddayArea;
    private TextView textDday;
    private TextView emptyStateHeader;
    private LinearLayout emptyStateContainer;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 생명주기 메서드_1 :: 초기 설정 메서드
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////////////////////////////////////////////////////////////////////////

        SharedPreferences userPref = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String Name = userPref.getString("user_name", "사용자명");
        String Email = userPref.getString("user_email", "이메일 없음");


        // 내 정보 레이아웃의 TextView에 값 설정
        TextView userNameView = findViewById(R.id.user_name); // 사용자 이름 TextView
        TextView userEmailView = findViewById(R.id.user_email); // 사용자 이메일 TextView
        userNameView.setText(Name); // 캐시된 사용자 이름 설정
        userEmailView.setText(Email); // 캐시된 사용자 이메일 설정


        // 내 정보 레이아웃의 TextView에 값 설정
        TextView userNameiew = findViewById(R.id.user_name2); // 사용자 이름 TextView
        userNameiew.setText(Name); // 캐시된 사용자 이름 설정


        ///////////////////////////////////////////////////////////////////////////////////////////////////



        // onCreate() 메서드 내부의 BottomSheet 초기화 부분
        bottomSheet = findViewById(R.id.bottom_sheet);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            setupBottomSheetCallback(); // 콜백 설정 추가

            // 초기 상태 설정
            SharedPreferences prefs = getSharedPreferences("BottomSheetState", MODE_PRIVATE);
            int savedState = prefs.getInt("bottom_sheet_state", BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Bottom Sheet의 루트 뷰 가져오기
        View bottomSheetView = findViewById(R.id.bottom_sheet);
        // Intent로 데이터 받아오기
        Intent intent = getIntent();

        // 현재 로그인한 사용자 이메일을 가져옵니다.
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userEmail = userPrefs.getString("user_email", "");

        // Retrofit으로 서버에서 데이터 가져오기
        RouteDetailRequest request = new RouteDetailRequest(userEmail);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getRouteDetails(request).enqueue(new Callback<RouteDetailResponse>() {
            @Override
            public void onResponse(Call<RouteDetailResponse> call, Response<RouteDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 서버에서 받아온 데이터
                    List<RouteDetailResponse.RouteDetail> routeDetails = response.body().getDetails();
                    List<String> selectedLocations = response.body().getAreaNames();

                    // Empty State UI 관련 뷰 초기화
                    intendedTravelDday = bottomSheetView.findViewById(R.id.intended_travel_dday);
                    ddayArea = bottomSheetView.findViewById(R.id.dday_area);
                    textDday = bottomSheetView.findViewById(R.id.text_dday);
                    emptyStateHeader = bottomSheetView.findViewById(R.id.empty_state_header);
                    emptyStateContainer = bottomSheetView.findViewById(R.id.empty_state_container);

                    // 지역명 받아오기
                    String locationText = (selectedLocations != null && !selectedLocations.isEmpty())
                            ? TextUtils.join(", ", selectedLocations)
                            : "지역 미선택";

                    // 날짜별로 데이터 정리
                    Map<String, List<RouteDetailResponse.RouteDetail>> dateGroupedDetails = new HashMap<>();
                    List<String> selectedDates = new ArrayList<>();

                    for (RouteDetailResponse.RouteDetail detail : routeDetails) {
                        String date = detail.getDate();
                        if (!selectedDates.contains(date)) {
                            selectedDates.add(date);
                        }

                        dateGroupedDetails.computeIfAbsent(date, k -> new ArrayList<>())
                                .add(detail);
                    }

                    Collections.sort(selectedDates); // 날짜 정렬

                    // DynamicTravelList 업데이트 및 D-day 계산
                    LinearLayout dynamicTravelList = findViewById(R.id.dynamic_travel_list);
                    final AtomicBoolean isShowingDday = new AtomicBoolean(true);
                    final AtomicBoolean isMarginLeft220 = new AtomicBoolean(true);

                    if (!selectedDates.isEmpty()) {
                        String targetDateStr = selectedDates.get(0);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

                        try {
                            Date targetDate = sdf.parse(targetDateStr);
                            Date currentDate = new Date();

                            Calendar targetCalendar = Calendar.getInstance();
                            targetCalendar.setTime(targetDate);
                            targetCalendar.set(Calendar.HOUR_OF_DAY, 0);
                            targetCalendar.set(Calendar.MINUTE, 0);
                            targetCalendar.set(Calendar.SECOND, 0);
                            targetCalendar.set(Calendar.MILLISECOND, 0);

                            Calendar currentCalendar = Calendar.getInstance();
                            currentCalendar.setTime(currentDate);
                            currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
                            currentCalendar.set(Calendar.MINUTE, 0);
                            currentCalendar.set(Calendar.SECOND, 0);
                            currentCalendar.set(Calendar.MILLISECOND, 0);
                            // D-Day 계산 및 초기 상태 설정
                            long diffInMillis = targetCalendar.getTimeInMillis() - currentCalendar.getTimeInMillis();
                            long dDay = diffInMillis / (1000 * 60 * 60 * 24);

                            final String dDayText = dDay > 0 ? "D-" + dDay : dDay == 0 ? "D-Day" : "D+" + Math.abs(dDay);
                            textDday.setText(dDayText);

                            textDday.setSingleLine(false);
                            textDday.setMaxLines(Integer.MAX_VALUE);
                            textDday.setEllipsize(null);
                            textDday.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

                            ddayArea.setVisibility(View.VISIBLE);
                            textDday.setVisibility(View.VISIBLE);

                            // 여행 일정 텍스트 설정
                            int numberOfDays = selectedDates.size();
                            int nights = numberOfDays - 1;
                            String travelText = locationText + "에서 " + nights + "박 " + numberOfDays + "일 여정기";
                            intendedTravelDday.setText(travelText);

                            // 여행 일정 텍스트 초기 설정
                            intendedTravelDday.setSingleLine(false);
                            intendedTravelDday.setMaxLines(Integer.MAX_VALUE);
                            intendedTravelDday.setEllipsize(null);

                            // D-Day 클릭 리스너 설정
                            textDday.setOnClickListener(view -> {
                                if (isShowingDday.get()) {
                                    // 텍스트를 날짜로 변경
                                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(targetDate);
                                    textDday.setText(formattedDate + " 예정");

                                    // 텍스트 잘림 방지를 위해 설정
                                    textDday.setSingleLine(false);
                                    textDday.setEllipsize(null);
                                    textDday.setMaxLines(Integer.MAX_VALUE);

                                    // 이미지 뷰 확장 (마진 왼쪽 줄이면서 폭을 늘림)
                                    FrameLayout.LayoutParams areaParams = (FrameLayout.LayoutParams) ddayArea.getLayoutParams();
                                    areaParams.leftMargin = dpToPx(210f); // 마진 줄이기
                                    areaParams.width = dpToPx(133f); // 폭 늘리기
                                    ddayArea.setLayoutParams(areaParams);

                                    // 텍스트 뷰 확장 (마진 왼쪽 줄이기)
                                    FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) textDday.getLayoutParams();
                                    textParams.leftMargin = dpToPx(225f); // 마진 줄이기
                                    textDday.setLayoutParams(textParams);
                                } else {
                                    // 텍스트를 D-Day로 복귀
                                    textDday.setText(dDayText);

                                    // 이미지 뷰 원래 크기와 위치로 복귀
                                    FrameLayout.LayoutParams areaParams = (FrameLayout.LayoutParams) ddayArea.getLayoutParams();
                                    areaParams.leftMargin = dpToPx(275.8f); // 원래 마진
                                    areaParams.width = dpToPx(66f); // 원래 폭
                                    ddayArea.setLayoutParams(areaParams);

                                    // 텍스트 뷰 원래 위치로 복귀
                                    FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) textDday.getLayoutParams();
                                    textParams.leftMargin = dpToPx(284.85f); // 원래 마진
                                    textDday.setLayoutParams(textParams);
                                }

                                // 상태 토글
                                isShowingDday.set(!isShowingDday.get());
                            });

                            // 날짜별 여행 일정 표시
                            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                            for (int i = 0; i < selectedDates.size(); i++) {
                                View tripView = inflater.inflate(R.layout.inc_main_route, dynamicTravelList, false);

                                TextView daytime = tripView.findViewById(R.id.daytime);
                                TextView dayTimeText = tripView.findViewById(R.id.day_time);

                                // day_time 텍스트 설정에도 제한 제거
                                dayTimeText.setSingleLine(false);
                                dayTimeText.setMaxLines(Integer.MAX_VALUE);
                                dayTimeText.setEllipsize(null);

                                ConstraintLayout detailRoute = tripView.findViewById(R.id.detail_route);
                                detailRoute.setOnClickListener(v -> {
                                    Intent intent = new Intent(MainActivity.this, DetailTrip.class);
                                    String selectedDayTime = dayTimeText.getText().toString();
                                    intent.putExtra("selected_day_time", selectedDayTime);
                                    startActivity(intent);
                                });

                                String dayNumber = String.valueOf(i + 1);
                                daytime.setText(dayNumber);
                                dayTimeText.setText(dayNumber);

                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.setMargins(-24, -30, -24, 10);
                                tripView.setLayoutParams(layoutParams);

                                String currentDatee = selectedDates.get(i);
                                List<RouteDetailResponse.RouteDetail> placesForDay = dateGroupedDetails.get(currentDatee);

                                if (placesForDay != null && !placesForDay.isEmpty()) {
                                    ConstraintLayout middleArea = tripView.findViewById(R.id.middle_area);
                                    setupMiddleArea(middleArea, placesForDay);
                                }

                                dynamicTravelList.addView(tripView);
                            }

                            ((View) dynamicTravelList.getParent()).setVisibility(View.VISIBLE);
                            emptyStateContainer.setVisibility(View.GONE);
                            emptyStateHeader.setVisibility(View.GONE);
                            intendedTravelDday.setVisibility(View.VISIBLE);
                            textDday.setVisibility(View.VISIBLE);

                        } catch (ParseException e) {
                            Log.e("MainActivity", "Date parsing failed: " + e.getMessage());
                        }
                    } else {
                        // 데이터가 없을 때 Empty State UI 설정
                        intendedTravelDday.setVisibility(View.INVISIBLE);
                        ddayArea.setVisibility(View.INVISIBLE);
                        textDday.setVisibility(View.INVISIBLE);
                        emptyStateHeader.setVisibility(View.VISIBLE);
                        emptyStateContainer.setVisibility(View.VISIBLE);
                    }


                } else {
                    // 서버 응답 실패 처리
                    Log.e("MainActivity", "Server response failed");
                    // 에러 처리 UI 업데이트
                }
            }

            @Override
            public void onFailure(Call<RouteDetailResponse> call, Throwable t) {
                // 네트워크 오류 처리
                Log.e("MainActivity", "Network request failed", t);
                // 에러 처리 UI 업데이트
            }
        });


        ////////////////////////////////////////////////////////////////////////


        // SharedPreferences 초기화 및 세션 ID 설정
        String sessionId = UUID.randomUUID().toString();
        userPrefs.edit().putString("chat_session_id", sessionId).apply();

        // 검색 영역 클릭 시 이벤트 설정
        findViewById(R.id.search_icon).setOnClickListener(v -> {
            Intent it = new Intent(MainActivity.this, TripPlanner1.class);
            startActivity(it);
        });

        // ChatHistory 초기화
        SharedPreferences chatSharedPreferences = getSharedPreferences("ChatHistory", MODE_PRIVATE);
        SharedPreferences.Editor editor = chatSharedPreferences.edit();
        editor.clear();
        editor.apply();

        // ImageView 초기화 및 로그 확인
        imageView = findViewById(R.id.imageView);
        if (imageView == null) {
            Log.e("MainActivity", "imageView 초기화 실패 - ID가 잘못되었거나 레이아웃에 존재하지 않습니다.");
        } else {
            Log.d("MainActivity", "imageView 초기화 성공: " + imageView.toString());
        }

        // ImageView 초기화
        ImageView imageView1 = findViewById(R.id.imageView1);
        ImageView imageView2 = findViewById(R.id.imageView2);

        // 애니메이션에 사용할 핸들러와 필요한 변수들
        Handler handler = new Handler();
        int[] images = {
                R.drawable.img_banner_gwangmyeong,
                R.drawable.img_banner_gyeongju_2
        }; // 순환할 이미지 목록
        int[] currentImageIndex = {0}; // 배열로 선언하여 수정 가능하도록 설정
        boolean[] isImageView1Visible = {true}; // 현재 보이는 ImageView를 추적

        // 이미지 페이드 전환 시작
        Runnable imageFadeRunnable = new Runnable() {
            @Override
            public void run() {
                // 다음 이미지 계산
                currentImageIndex[0] = (currentImageIndex[0] + 1) % images.length;
                int nextImageRes = images[currentImageIndex[0]];

                // 현재 보이는 ImageView를 기준으로 페이드인/페이드아웃 실행
                if (isImageView1Visible[0]) {
                    imageView2.setImageResource(nextImageRes);
                    fadeImages(imageView1, imageView2);
                } else {
                    imageView1.setImageResource(nextImageRes);
                    fadeImages(imageView2, imageView1);
                }

                // 보이는 ImageView 전환
                isImageView1Visible[0] = !isImageView1Visible[0];

                // 5초 후 다시 실행
                handler.postDelayed(this, 5000);
            }
        };

        // 처음 실행
        handler.postDelayed(imageFadeRunnable, 5000);

        ////////////////////////////////////////////////////////////////////////////////////////////////

        //메인페이지 포스터 동적 생성
        // 횡스크롤 뷰 내부 콘텐츠 영역
        LinearLayout scrollContent = findViewById(R.id.scroll_content);

// string-array 리소스에서 데이터 가져오기
        String[] posterTitles = getResources().getStringArray(R.array.poster_title);
        TypedArray posterImages = getResources().obtainTypedArray(R.array.poster_image);

// 동적으로 콘텐츠 추가
        for (int i = 0; i < posterTitles.length; i++) {
            String title = posterTitles[i];
            int imageResId = posterImages.getResourceId(i, -1);

            // 콘텐츠 뷰 inflate
            View posterView = LayoutInflater.from(this).inflate(R.layout.inc_season_recommend_poster_layout, scrollContent, false);

            // 이미지와 텍스트 설정
            ImageView posterImage = posterView.findViewById(R.id.poster_image);
            TextView posterTitle = posterView.findViewById(R.id.poster_title);

            posterImage.setImageResource(imageResId);
            posterTitle.setText(title);

            // 클릭 이벤트 설정
            posterView.setOnClickListener(v -> {
                // PopularAreaActivity로 제목 전달
                Intent it = new Intent(MainActivity.this, PopularAreaActivity.class);
                it.putExtra("area_name", title); // 제목 전달
                startActivity(it);
            });

            // 스크롤 콘텐츠 영역에 추가
            scrollContent.addView(posterView);
        }

        // TypedArray는 재사용 후 recycle 필수
        posterImages.recycle();

        ////////////////////////////////////////////////////////////////////////////////////////////////

        //바텀시트 영역


        // BottomSheet 설정
        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Bottom Sheet의 루트 뷰 가져오기

        // Intent로 데이터 받아오기
        Intent it = getIntent();
        Gson gson = new Gson();
        String selectedDatesJson = it.getStringExtra("selectedDates");
        Type dateListType = new TypeToken<ArrayList<Date>>() {
        }.getType();
        ArrayList<Date> selectedDates = gson.fromJson(selectedDatesJson, dateListType);

        String dayWiseDestinationsJson = it.getStringExtra("dayWiseDestinations");
        Type mapType = new TypeToken<HashMap<Integer, ArrayList<String>>>() {
        }.getType();
        HashMap<Integer, ArrayList<String>> dayWiseDestinations = gson.fromJson(dayWiseDestinationsJson, mapType);


        // SharedPreferences에서 사용자명 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userName = sharedPreferences.getString("user_name", "사용자명"); // 저장된 이름 없을 경우 기본값 "사용자명"

        // 여행 데이터 불러오기
        SharedPreferences travelDataPrefs = getSharedPreferences("TravelData", MODE_PRIVATE);


        // Empty State UI 관련 뷰 초기화
        intendedTravelDday = bottomSheetView.findViewById(R.id.intended_travel_dday);
        ddayArea = bottomSheetView.findViewById(R.id.dday_area);
        textDday = bottomSheetView.findViewById(R.id.text_dday);
        emptyStateHeader = bottomSheetView.findViewById(R.id.empty_state_header);
        emptyStateContainer = bottomSheetView.findViewById(R.id.empty_state_container);


        // 기존 코드와 동일하게 dynamicTravelList 업데이트
        LinearLayout dynamicTravelList = findViewById(R.id.dynamic_travel_list);
        if (selectedDates != null && !selectedDates.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < selectedDates.size(); i++) {
                View tripView = inflater.inflate(R.layout.inc_main_route, dynamicTravelList, false);

                TextView daytime = tripView.findViewById(R.id.daytime);
                TextView dayTimeText = tripView.findViewById(R.id.day_time);

                String dayNumber = String.valueOf(i + 1);
                daytime.setText(dayNumber);
                dayTimeText.setText(dayNumber);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(-24, -30, -24, 10);
                tripView.setLayoutParams(layoutParams);

                List<String> placesForDay = dayWiseDestinations.get(i + 1);
                if (placesForDay != null && !placesForDay.isEmpty()) {
                    ConstraintLayout middleArea = tripView.findViewById(R.id.middle_area);

                    middleArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            middleArea.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            int numberOfPlaces = placesForDay.size();
                            int availableWidth = middleArea.getWidth();
                            List<ImageView> flags = new ArrayList<>();

                            for (int j = 0; j < numberOfPlaces; j++) {
                                String placeInfo = placesForDay.get(j);
                                String[] parts = placeInfo.split(",");
                                String placeName = parts[0];

                                ImageView flag = new ImageView(MainActivity.this);
                                flag.setId(View.generateViewId());
                                flag.setImageResource(R.drawable.drawable_red_flag);

                                ConstraintLayout.LayoutParams flagParams = new ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                                );
                                flagParams.topToTop = middleArea.getId();
                                flagParams.bottomToBottom = middleArea.getId();
                                if (numberOfPlaces == 1) {
                                    flagParams.startToStart = middleArea.getId();
                                    flagParams.endToEnd = middleArea.getId();
                                } else if (numberOfPlaces == 2) {
                                    if (j == 0) {
                                        flagParams.startToStart = middleArea.getId();
                                        flagParams.leftMargin = dpToPx(20);  // 왼쪽 여백 20dp
                                    } else {
                                        flagParams.endToEnd = middleArea.getId();
                                        flagParams.rightMargin = dpToPx(20); // 오른쪽 여백 20dp
                                    }
                                } else {
                                    int availableWidthWithMargin = availableWidth - dpToPx(60);
                                    int spacing = availableWidthWithMargin / (numberOfPlaces - 1);

                                    flagParams.leftMargin = dpToPx(20) + j * spacing;
                                    flagParams.startToStart = middleArea.getId();
                                }

                                middleArea.addView(flag, flagParams);
                                flags.add(flag);

                                TextView placeText = new TextView(MainActivity.this);
                                placeText.setId(View.generateViewId());
                                placeText.setText(placeName);
                                placeText.setTextSize(12);
                                placeText.setGravity(Gravity.CENTER);
                                placeText.setEllipsize(TextUtils.TruncateAt.END);
                                placeText.setMaxLines(1);
                                placeText.setWidth(dpToPx(50));

                                ConstraintLayout.LayoutParams textParams = new ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                                );
                                textParams.topToBottom = flag.getId();
                                textParams.topMargin = dpToPx(10);
                                textParams.startToStart = flag.getId();
                                textParams.endToEnd = flag.getId();

                                middleArea.addView(placeText, textParams);
                            }

                            for (int j = 0; j < flags.size() - 1; j++) {
                                ImageView startFlag = flags.get(j);
                                ImageView endFlag = flags.get(j + 1);

                                View line = new View(MainActivity.this);
                                line.setBackgroundColor(Color.GRAY);

                                ConstraintLayout.LayoutParams lineParams = new ConstraintLayout.LayoutParams(
                                        0,
                                        dpToPx(2)
                                );

                                lineParams.bottomToBottom = startFlag.getId();
                                lineParams.startToStart = startFlag.getId();
                                lineParams.endToStart = endFlag.getId();
                                lineParams.leftMargin = dpToPx(3);

                                middleArea.addView(line, lineParams);
                            }
                        }
                    });
                }

                dynamicTravelList.addView(tripView);
                ((View) dynamicTravelList.getParent()).setVisibility(View.VISIBLE);
                emptyStateContainer.setVisibility(View.GONE);
                emptyStateHeader.setVisibility(View.GONE);
                intendedTravelDday.setVisibility(View.VISIBLE);
            }
        } else {
            intendedTravelDday.setVisibility(View.INVISIBLE);
            ddayArea.setVisibility(View.INVISIBLE);
            textDday.setVisibility(View.INVISIBLE);
            emptyStateHeader.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        }


        recommendedPlaces = findViewById(R.id.recommended_places);
        options_area = findViewById(R.id.options_area);
        View mainView = findViewById(R.id.main); // Bottom Sheet를 포함하는 부모 뷰

        // 뷰의 레이아웃이 완료된 후에 크기를 계산하도록 보장
        mainView.post(() -> {
            int parentHeight = mainView.getHeight();
            int recommendPlaceBottom = recommendedPlaces.getBottom();
            int searchAreaBottom = options_area.getBottom();

            // peekHeight와 expandedOffset 계산
            int peekHeight = parentHeight - searchAreaBottom;
            int expandedOffset = recommendPlaceBottom;


            // 계산된 값 로그로 출력하여 확인 (디버깅 용도)
            Log.d("MainActivity", "Parent Height: " + parentHeight);
            Log.d("MainActivity", "searchArea Bottom: " + searchAreaBottom);
            Log.d("MainActivity", "SearchArea Bottom: " + recommendPlaceBottom);
            Log.d("MainActivity", "Peek Height: " + peekHeight);
            Log.d("MainActivity", "Expanded Offset: " + expandedOffset);

            // peekHeight와 expandedOffset 설정
            if (peekHeight < 0) peekHeight = 0;
            if (expandedOffset < 0) expandedOffset = 0;

            bottomSheetBehavior.setPeekHeight(peekHeight, true);
            bottomSheetBehavior.setExpandedOffset(expandedOffset);

            // Bottom Sheet가 숨겨지지 않고 콘텐츠 높이에 맞추지 않도록 설정
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setFitToContents(false);
        });

        // Bottom Sheet 콜백 설정
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 중간 상태 없이 최소화 및 최대 확장 상태로 전환
                if (slideOffset < 0.5f) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });


        // btn_add_trip의 터치 이벤트 설정
        ConstraintLayout btnAddTrip = findViewById(R.id.btn_add_trip);
        btnAddTrip.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 터치 시작 시 elevation을 0으로 설정하고 배경 변경
                    view.setElevation(0f);
                    view.setBackgroundResource(R.drawable.drawable_skyblue_rectangle); // 눌린 상태의 배경
                    break;

                case MotionEvent.ACTION_UP:
                    // 터치 종료 시 elevation을 원래 값으로 복원하고 배경 복구
                    view.setElevation(6f); // 기본 elevation
                    view.setBackgroundResource(R.drawable.drawable_skyblue_rectangle); // 원래 상태의 배경

                    // TripPlanner1 액티비티로 이동
                    Intent itt = new Intent(MainActivity.this, TripPlanner1.class);
                    startActivity(itt);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    // 터치가 취소된 경우에도 원래 상태로 복구
                    view.setElevation(3f);
                    view.setBackgroundResource(R.drawable.drawable_blue_rectangle);
                    break;

            }
            return true;
        });
        ////////////////////////////////////////////////////////////////////////////////////////////////


        //8버튼 터치 이펙트
        containerButton8_1 = findViewById(R.id.container_button8_1);
        containerButton8_2 = findViewById(R.id.container_button8_2);
        containerButton8_3 = findViewById(R.id.container_button8_3);
        containerButton8_4 = findViewById(R.id.container_button8_4);
        containerButton8_5 = findViewById(R.id.container_button8_5);
        containerButton8_6 = findViewById(R.id.container_button8_6);
        containerButton8_7 = findViewById(R.id.container_button8_7);
        containerButton8_8 = findViewById(R.id.container_button8_8);

        setTouchEventListeners();


        ////////////////////////////////////////////////////////////////////////////////////////////

        //메뉴창
        // DrawerLayout 설정
        drawerLayout = findViewById(R.id.drawer_layout);

        // BottomSheet 초기 상태 설정
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(230, true);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setExpandedOffset(120);

        // include된 activity_option.xml 내부의 레이아웃 참조
        NavigationView navigationView = findViewById(R.id.navigation_view);
        View drawerContent = navigationView.findViewById(R.id.drawer_content);
        myPrivacyLayout = drawerContent.findViewById(R.id.my_privacy);
        myChallengeLayout = drawerContent.findViewById(R.id.my_challenge_layout);
        myReviewLayout = drawerContent.findViewById(R.id.my_review_layout);




        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // 사용자 이름 가져오기
        String currentUserName = preferences.getString("user_name", "사용자");
        boolean isFirstRun = preferences.getBoolean("is_first_run_" + currentUserName, true);

        if (isFirstRun) {
            // 환영 메시지 표시
            showSnackbar(currentUserName + "님, 환영합니다!");

            // 환영 메시지를 표시한 후 플래그를 false로 변경
            SharedPreferences.Editor editor2 = preferences.edit();
            editor2.putBoolean("is_first_run_" + currentUserName, false);
            editor2.apply();
        }



        // 버튼 레이아웃 참조
        containerButton1 = drawerContent.findViewById(R.id.container_button1);

        // 기본값 -> my_privacy
        showLayout(myPrivacyLayout);
        containerButton1.setBackgroundColor(ContextCompat.getColor(this, R.color.clicked_background_color));
        lastSelectedLayout = myPrivacyLayout;

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // 드로어가 슬라이드될 때 호출됨 (필요하지 않음)
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // 드로어가 열릴 때 호출됨 (필요하지 않음)
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // 드로어가 닫힐 때 호출됨
                // 항상 기본 레이아웃(myPrivacyLayout)을 표시
                showLayout(myPrivacyLayout); // 애니메이션 없이 바로 레이아웃 설정
                containerButton1.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.clicked_background_color));
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // 드로어 상태가 변경될 때 호출됨 (필요하지 않음)
            }
        });

        // 메뉴 아이콘 클릭 시 드로어 열기
        findViewById(R.id.menu_icon).setOnClickListener(view -> {
            drawerLayout.openDrawer(findViewById(R.id.navigation_view));
        });

        // 내 정보 버튼 클릭 시
        containerButton1.setOnClickListener(view -> {
            switchLayout(myPrivacyLayout, containerButton1, containerButton2, containerButton3);
        });


        // X 버튼 클릭 시 드로어 닫기
        drawerContent.findViewById(R.id.ic_quit).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            }
        });

        // X 버튼 클릭 시 드로어 닫기
        findViewById(R.id.ic_quit).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(findViewById(R.id.navigation_view))) {
                drawerLayout.closeDrawer(findViewById(R.id.navigation_view));
            }

        });

        ////////////////////////////////////////////////////////////////////////////////////////
        // logout 버튼 참조
        FrameLayout logoutButton = findViewById(R.id.logout);

        // 로그아웃 버튼 클릭 시 동작 설정
        logoutButton.setOnClickListener(logoutView -> {
            performLogout();
        });
        ////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////

        // 애니메이션 대상 뷰들 초기화
        bottomSheet = findViewById(R.id.bottom_sheet);
        recommendedPlaces = findViewById(R.id.recommended_places);
        search_icon = findViewById(R.id.search_icon);


        // X 버튼 클릭 시 드로어 닫기
        findViewById(R.id.ic_quit).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(findViewById(R.id.navigation_view))) {
                drawerLayout.closeDrawer(findViewById(R.id.navigation_view));
            }
        });

        // 메뉴 아이콘 클릭 시 드로어 열기
        findViewById(R.id.menu_icon).setOnClickListener(view -> {
            drawerLayout.openDrawer(findViewById(R.id.navigation_view));
        });

        // 내 정보 버튼 클릭 시
        containerButton1.setOnClickListener(view -> {
            switchLayout(myPrivacyLayout, containerButton1, containerButton2, containerButton3);
        });


        // X 버튼 클릭 시 드로어 닫기
        drawerContent.findViewById(R.id.ic_quit).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            }
        });

        // 인기 지역 버튼 설정 부분 수정
        FrameLayout popularArea = findViewById(R.id.popular_area);
        FrameLayout popularArea1 = findViewById(R.id.popular_area1);
        FrameLayout popularArea2 = findViewById(R.id.popular_area2);
        FrameLayout popularArea3 = findViewById(R.id.popular_area3);


        // 각 지역별 클릭 리스너 설정
        setTouchAndClickListener(popularArea, imagePopular, "속초");
        setTouchAndClickListener(popularArea1, imagePopular1, "부산");
        setTouchAndClickListener(popularArea2, imagePopular2, "제주도");
        setTouchAndClickListener(popularArea3, imagePopular3, "서울");

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void performLogout() {
        // SharedPreferences 초기화
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // is_logged_in 플래그를 false로 설정
        editor.putBoolean("is_logged_in", false);

        // 저장된 사용자 데이터 삭제 (선택)
        editor.remove("user_name");
        editor.remove("user_email");
        editor.apply();

        // 로그인 화면으로 이동하면서 메시지 전달
        Intent intent = new Intent(MainActivity.this, Introductory.class);
        intent.putExtra("snackbar_message", "안녕히 가세요!");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 현재 액티비티 종료
        finish();
    }


    private void setTouchAndClickListener(FrameLayout frameLayout, ImageView imageView, String areaName) {
        frameLayout.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                // PopularAreaActivity로 지역명을 전달
                Intent it_popular = new Intent(MainActivity.this, PopularAreaActivity.class);
                it_popular.putExtra("area_name", areaName); // 지역명 전달
                startActivity(it_popular);
            }
            return true;
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 생명주기 메서드_2 :: 액티비티 재개 시 실행되는 메서드
    @Override
    protected void onResume() {
        super.onResume();
        setupClickListeners();

        SharedPreferences prefs = getSharedPreferences("BottomSheetState", MODE_PRIVATE);
        int savedState = prefs.getInt("bottom_sheet_state", BottomSheetBehavior.STATE_COLLAPSED);

        if (bottomSheetBehavior != null) {
            // Only set valid states
            if (savedState == BottomSheetBehavior.STATE_COLLAPSED ||
                    savedState == BottomSheetBehavior.STATE_EXPANDED ||
                    (savedState == BottomSheetBehavior.STATE_HIDDEN && bottomSheetBehavior.isHideable())) {
                bottomSheetBehavior.setState(savedState);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 생명주기 메서드_3 :: 일시정지 시 실행되는 메서드
    @Override
    protected void onPause() {
        super.onPause();

        // BottomSheet 상태 저장
        if (bottomSheetBehavior != null) {
            SharedPreferences prefs = getSharedPreferences("BottomSheetState", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("bottom_sheet_state", bottomSheetBehavior.getState());
            editor.apply();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 생명주기 메서드_4 :: 액티비티 종료 시 실행되는 메서드
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Window 관련 리소스 정리
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );


    }
    ////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 레이아웃 전환 메서드_1 :: 선택된 레이아웃만 표시하는 메서드
    private void showLayout(View layoutToShow) {
        myPrivacyLayout.setVisibility(View.GONE);
        myChallengeLayout.setVisibility(View.GONE);
        myReviewLayout.setVisibility(View.GONE);

        layoutToShow.setVisibility(View.VISIBLE);
        lastSelectedLayout = layoutToShow;
    }

    // 레이아웃 전환 메서드_2 :: 레이아웃 전환 및 버튼 상태 변경
    private void switchLayout(View layoutToShow, View clickedButton, View... otherButtons) {
        // 애니메이션 적용
        applyAnimation(layoutToShow);

        // 레이아웃 가시성 설정
        showLayout(layoutToShow);

        // 클릭된 버튼의 배경색 변경
        clickedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.clicked_background_color));

        // 나머지 버튼들의 배경색을 기본값으로 설정
        for (View button : otherButtons) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.default_background_color));
        }
    }

    // 레이아웃 전환 메서드_3 :: 레이아웃 전환 애니메이션 적용
    private void applyAnimation(View layoutToShow) {
        if (lastSelectedLayout != layoutToShow) {
            if (layoutToShow == myPrivacyLayout && lastSelectedLayout == myChallengeLayout) {
                // 애니메이션 설정 (필요 시)
            } else if (layoutToShow == myChallengeLayout && lastSelectedLayout == myReviewLayout) {
                // 애니메이션 설정 (필요 시)
            } else if (layoutToShow == myReviewLayout && lastSelectedLayout == myPrivacyLayout) {
                // 애니메이션 설정 (필요 시)
            } else if (layoutToShow == myPrivacyLayout && lastSelectedLayout == myReviewLayout) {
                // 애니메이션 설정 (필요 시)
            } else {
                // 기본 애니메이션 설정 (필요 시)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////////////////

    // 터치 이벤트를 설정하는 메서드
    private void setTouchEventListeners() {
        // 영화 출연지 등의 명소
        setTouchEffectWithThemes(containerButton8_1, "유명 핫플레이스",
                Arrays.asList("사진 여행", "야경 투어", "핫플 투어", "문화 답사"));

        // 역사적 장소
        setTouchEffectWithThemes(containerButton8_2, "역사적 장소",
                Arrays.asList("역사 투어", "문화 답사", "전통 체험", "도시 야경"));

        // 자전거 여행 명소
        setTouchEffectWithThemes(containerButton8_3, "자전거 여행 명소",
                Arrays.asList("해안 자전거", "트래킹", "바닷길 드라이브", "스포츠 여행"));

        // 도보 20마일 여행
        setTouchEffectWithThemes(containerButton8_4, "도보 20마일 여행",
                Arrays.asList("걷기 코스", "트래킹", "숲속 캠핑", "산악 탐방"));

        // 사진촬영 명소
        setTouchEffectWithThemes(containerButton8_5, "사진촬영 명소",
                Arrays.asList("사진 여행", "야경 투어", "사진 예술", "풍경 투어"));

        // 유명, 분위기 좋은 카페들
        setTouchEffectWithThemes(containerButton8_6, "유명, 분위기 좋은 카페들",
                Arrays.asList("커피 와인", "힐링 스팟", "로컬 푸드", "로맨틱 산책"));

        // 겨울여행 필수 여행지
        setTouchEffectWithThemes(containerButton8_7, "겨울여행 필수 여행지",
                Arrays.asList("겨울 여행", "스키 투어", "바다 온천", "겨울 캠핑"));

        // 레저, 캠핑
        setTouchEffectWithThemes(containerButton8_8, "레저, 캠핑",
                Arrays.asList("숲속 캠핑", "패러글라이딩", "하이킹 코스", "해양 스포츠"));
    }

    private void setTouchEffectWithThemes(View containerButton, String buttonName, List<String> themes) {
        containerButton.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;

                case MotionEvent.ACTION_UP:

                    // ThemeRouteActivity로 이동하면서 테마 정보 전달
                    Intent intent = new Intent(MainActivity.this, ThemeRouteActivity.class);
                    intent.putExtra("buttonName", buttonName);
                    intent.putStringArrayListExtra("themes", new ArrayList<>(themes));
                    startActivity(intent);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    break;
            }
            return true;
        });
    }

    private void fadeImages(ImageView fadingOut, ImageView fadingIn) {
        // 초기 상태 설정
        fadingIn.setAlpha(0f);
        fadingIn.setVisibility(View.VISIBLE);

        // 애니메이션 객체 설정
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(fadingOut, "alpha", 1f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(fadingIn, "alpha", 0f, 1f);

        // 애니메이션 세트로 실행
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeOut, fadeIn);
        animatorSet.setDuration(1000); // 1초간 전환
        animatorSet.start();

        // 애니메이션 종료 후 처리
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fadingOut.setVisibility(View.GONE);
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void setupClickListeners() {
        // 이름과 이메일 섹션의 클릭 리스너 설정
        FrameLayout nameSection = findViewById(R.id.my_name);
        FrameLayout emailSection = findViewById(R.id.my_email);

        // 이름 영역 클릭 시 다이얼로그 표시
        nameSection.setOnClickListener(v -> showInfoDialog(true));

        // 이메일 영역 클릭 시 다이얼로그 표시
        emailSection.setOnClickListener(v -> showInfoDialog(false));
    }

    private void showInfoDialog(boolean isNameVisible) {
        // 다이얼로그 설정
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.inc_option_my_info); // 다이얼로그 레이아웃 사용
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // 다이얼로그 내부 뷰 참조
        FrameLayout logoutSection = dialog.findViewById(R.id.logout);
        FrameLayout addInfoSection1 = dialog.findViewById(R.id.add_info);
        FrameLayout addInfoSection2 = dialog.findViewById(R.id.add_info2);
        FrameLayout nameSection = dialog.findViewById(R.id.my_name);
        FrameLayout emailSection = dialog.findViewById(R.id.my_email);
        ImageView dividerLine = dialog.findViewById(R.id.divider_line); // 다이얼로그 내부에서 참조

        // 불필요한 섹션 숨기기
        if (logoutSection != null) logoutSection.setVisibility(View.GONE);
        if (addInfoSection1 != null) addInfoSection1.setVisibility(View.GONE);
        if (addInfoSection2 != null) addInfoSection2.setVisibility(View.GONE);

        if (isNameVisible) {
            nameSection.setVisibility(View.VISIBLE);
            emailSection.setVisibility(View.GONE);
        } else {
            nameSection.setVisibility(View.GONE);
            emailSection.setVisibility(View.VISIBLE);

            // 이메일 영역 클릭 시 divider_line 숨기기
            if (dividerLine != null) dividerLine.setVisibility(View.GONE);

            // 이메일 영역을 상단으로 이동
            FrameLayout.LayoutParams emailLayoutParams = (FrameLayout.LayoutParams) emailSection.getLayoutParams();
            emailLayoutParams.topMargin = 0; // 이메일 영역을 상단으로 이동
            emailSection.setLayoutParams(emailLayoutParams);
        }

        // SharedPreferences에서 사용자 정보 불러오기
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userEmail = userPrefs.getString("user_email", "");
        String userName = userPrefs.getString("user_name", "사용자명");

        // 다이얼로그의 TextView에 초기 데이터 설정
        TextView nameTextView = dialog.findViewById(R.id.user_name2);
        TextView emailTextView = dialog.findViewById(R.id.user_email);

        nameTextView.setText(userName);
        emailTextView.setText(userEmail);

        // EditText를 동적으로 생성하여 TextView 클릭 시 EditText로 변환
        EditText nameEditText = new EditText(this);
        EditText emailEditText = new EditText(this);

        if (isNameVisible) {
            setupEditableField(nameTextView, nameEditText, "이름을 입력하세요");
        } else {
            setupEditableField(emailTextView, emailEditText, "이메일을 입력하세요");
        }

        // 저장 버튼 설정 및 클릭 시 서버에 값 업데이트 후 SharedPreferences에 저장
        Button saveButton = dialog.findViewById(R.id.modifybutton);
        saveButton.setVisibility(View.VISIBLE);
        saveButton.setOnClickListener(v -> {
            String updatedName = nameEditText.getText().toString();
            String updatedEmail = emailEditText.getText().toString();

            // 선택적으로 수정된 값만 업데이트
            if (isNameVisible && !updatedName.isEmpty()) {
                updateUserInfo(userPrefs, updatedName, null, saveButton, dialog);
            } else if (!isNameVisible && !updatedEmail.isEmpty()) {
                updateUserInfo(userPrefs, null, updatedEmail, saveButton, dialog);
            }
        });

        dialog.show(); // 다이얼로그 표시
    }

    private void updateUserInfo(SharedPreferences userPrefs, String updatedName, String updatedEmail, Button saveButton, Dialog dialog) {
        String userName = updatedName != null ? updatedName : userPrefs.getString("user_name", "");
        String userEmail = updatedEmail != null ? updatedEmail : userPrefs.getString("user_email", "");

        // Retrofit 요청 객체 생성
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(userName, userEmail);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        retrofit2.Call<UpdateUserResponse> call = apiService.updateUserInfo(updateUserRequest);

        call.enqueue(new retrofit2.Callback<UpdateUserResponse>() {
            @Override
            public void onResponse(retrofit2.Call<UpdateUserResponse> call, retrofit2.Response<UpdateUserResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    // 업데이트 성공 시 SharedPreferences에 저장
                    SharedPreferences.Editor editor = userPrefs.edit();
                    if (updatedName != null) editor.putString("user_name", updatedName);
                    if (updatedEmail != null) editor.putString("user_email", updatedEmail);
                    editor.apply();

                    // UI 갱신
                    updateUserInfoInView();
                    Snackbar.make(findViewById(android.R.id.content), "수정이 완료되었습니다", Snackbar.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Snackbar.make(saveButton, "업데이트 실패", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<UpdateUserResponse> call, Throwable t) {
                Snackbar.make(saveButton, "서버 연결 실패: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // SharedPreferences 정보로 UI 업데이트
    private void updateUserInfoInView() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userName = userPrefs.getString("user_name", "사용자명");
        String userEmail = userPrefs.getString("user_email", "이메일 없음");

        TextView userNameView = findViewById(R.id.user_name);
        TextView userNameView2 = findViewById(R.id.user_name2);
        TextView userEmailView = findViewById(R.id.user_email);

        userNameView.setText(userName);
        userNameView2.setText(userName);
        userEmailView.setText(userEmail);
    }

    // setupEditableField 메서드 수정
    private void setupEditableField(TextView textView, EditText editText, String hintMessage) {
        // EditText의 레이아웃을 TextView와 동일하게 설정
        editText.setLayoutParams(textView.getLayoutParams());
        editText.setText(textView.getText()); // 기존 텍스트를 EditText로 복사
        editText.setTextColor(textView.getCurrentTextColor());
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize());

        // TextView의 스타일을 최대한 반영
        editText.setPadding(
                textView.getPaddingLeft(),
                textView.getPaddingTop(),
                textView.getPaddingRight(),
                textView.getPaddingBottom()
        );
        editText.setGravity(textView.getGravity());
        editText.setBackground(null);

        // TextView와의 줄 간격 맞추기
        editText.setLineSpacing(textView.getLineSpacingExtra(), textView.getLineSpacingMultiplier());

        textView.setOnClickListener(v -> {
            // 클릭 시 EditText로 전환
            ViewGroup parent = (ViewGroup) textView.getParent();
            int index = parent.indexOfChild(textView);
            parent.removeView(textView);
            parent.addView(editText, index);
            editText.requestFocus();
        });

        // EditText의 포커스가 사라지면 TextView로 복원
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                String newText = editText.getText().toString().trim();
                if (!newText.isEmpty()) {
                    textView.setText(newText);
                    ViewGroup parent = (ViewGroup) editText.getParent();
                    int index = parent.indexOfChild(editText);
                    parent.removeView(editText);
                    parent.addView(textView, index);
                } else {
                    editText.setHint(hintMessage); // 내용이 없으면 힌트 제공
                    editText.requestFocus();
                }
            }
        });
    }

    // BottomSheet 콜백에서 상태 변경 시 저장
    private void setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // 상태 변경 시 저장
                SharedPreferences prefs = getSharedPreferences("BottomSheetState", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("bottom_sheet_state", newState);
                editor.apply();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 기존 슬라이드 로직 유지
                if (slideOffset < 0.5f) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
    }

    private void setupMiddleArea(ConstraintLayout middleArea, List<RouteDetailResponse.RouteDetail> placesForDay) {

        middleArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                middleArea.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int numberOfPlaces = placesForDay.size();
                int availableWidth = middleArea.getWidth();
                List<ImageView> flags = new ArrayList<>();

                // 시작 위치 조정을 위한 변수
                int totalWidth = dpToPx(20); // 시작 여백

                for (int j = 0; j < numberOfPlaces; j++) {
                    RouteDetailResponse.RouteDetail placeDetail = placesForDay.get(j);
                    String placeName = placeDetail.getPlaceName(); // 필요한 데이터 추출

                    // 깃발 추가
                    ImageView flag = new ImageView(MainActivity.this);
                    flag.setId(View.generateViewId());
                    flag.setImageResource(R.drawable.drawable_red_flag);

                    ConstraintLayout.LayoutParams flagParams = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    );
                    flagParams.topToTop = middleArea.getId();
                    flagParams.topMargin  =  dpToPx(20);

                    // 깃발 Z-Index를 높게 설정
                    flag.setTranslationZ(10); // 깃발을 앞으로 이동

                    if (numberOfPlaces == 1) {
                        flagParams.startToStart = middleArea.getId();
                        flagParams.endToEnd = middleArea.getId();
                        // 장소가 1개라면 중앙에 배치
                    } else if (numberOfPlaces == 2) {
                        if (j == 0) {
                            flagParams.startToStart = middleArea.getId();
                            flagParams.leftMargin = dpToPx(30);  // 왼쪽 여백 20dp
                        } else {
                            flagParams.endToEnd = middleArea.getId();
                            flagParams.rightMargin = dpToPx(30); // 오른쪽 여백 20dp
                        }
                    }else if(numberOfPlaces <= 4){
                        int availableWidthWithMargin = availableWidth - dpToPx(70);
                        int spacing = availableWidthWithMargin / (numberOfPlaces - 1);
                        flagParams.leftMargin = dpToPx(20) + j * spacing;
                        flagParams.startToStart = middleArea.getId();
                        // 장소가 3개 이상일 경우 일정 간격으로 플래그를 배치
                    }else {
                        int fixedSpacing = dpToPx(80); // 깃발 사이의 고정 간격을 30dp로 설정
                        int startMargin = dpToPx(30); // 시작 마진은 20dp로 설정
                        flagParams.startToStart = middleArea.getId();
                        flagParams.leftMargin = startMargin + j * fixedSpacing;
                        // 장소가 3개 이상일 경우 고정된 간격으로 플래그를 배치
                    }



                    middleArea.addView(flag, flagParams);
                    flags.add(flag);

                    // 깃발을 항상 앞으로 가져오기
                    flag.bringToFront();

                    // 깃발 아래 이름 텍스트 추가
                    TextView placeText = new TextView(MainActivity.this);
                    placeText.setId(View.generateViewId());
                    placeText.setText(placeName);
                    placeText.setTextSize(12);
                    placeText.setGravity(Gravity.CENTER);
                    placeText.setEllipsize(TextUtils.TruncateAt.END);
                    placeText.setMaxLines(1);
                    placeText.setWidth(dpToPx(50));

                    ConstraintLayout.LayoutParams textParams = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    );
                    textParams.topToBottom = flag.getId();
                    textParams.topMargin = dpToPx(10);
                    textParams.startToStart = flag.getId();
                    textParams.endToEnd = flag.getId();

                    middleArea.addView(placeText, textParams);

                    // 말풍선 텍스트뷰 추가 및 초기 숨김 설정
                    TextView bubbleText = new TextView(MainActivity.this);
                    bubbleText.setId(View.generateViewId());
                    bubbleText.setText(placeName);
                    bubbleText.setTextSize(14);
                    bubbleText.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
                    bubbleText.setBackgroundResource(R.drawable.bubble_background); // 말풍선 배경 리소스 설정
                    bubbleText.setVisibility(View.GONE);

                    ConstraintLayout.LayoutParams bubbleParams = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    bubbleParams.topToBottom = flag.getId();
                    bubbleParams.startToStart = flag.getId();
                    bubbleParams.endToEnd = flag.getId();
                    bubbleParams.topMargin = dpToPx(10);

                    middleArea.addView(bubbleText, bubbleParams);

                    // 깃발 클릭 이벤트 설정
                    flag.setOnClickListener(new View.OnClickListener() {
                        boolean isBubbleVisible = false;

                        @Override
                        public void onClick(View v) {
                            if (isBubbleVisible) {
                                bubbleText.setVisibility(View.GONE);
                            } else {
                                bubbleText.setVisibility(View.VISIBLE);
                            }
                            isBubbleVisible = !isBubbleVisible;
                        }
                    });

                    // 깃발 간 선 추가
                    for (int i = 0; i < flags.size() - 1; i++) {
                        ImageView startFlag = flags.get(i);
                        ImageView endFlag = flags.get(i + 1);

                        View line = new View(MainActivity.this);
                        line.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.skyblue)); // 선 색상 설정

                        ConstraintLayout.LayoutParams lineParams = new ConstraintLayout.LayoutParams(
                                0,
                                dpToPx(6)
                        );

                        lineParams.bottomToBottom = endFlag.getId();
                        lineParams.startToStart = startFlag.getId();
                        lineParams.endToEnd = endFlag.getId();
                        lineParams.rightMargin = dpToPx(11);
                        lineParams.leftMargin = dpToPx(11);
                        line.setLayoutParams(lineParams);
                        middleArea.addView(line, lineParams);
                    }
                }

                // 전체 레이아웃의 가로 크기를 설정하여 스크롤 가능하게 만듦
                ViewGroup.LayoutParams layoutParams = middleArea.getLayoutParams();
                layoutParams.width = totalWidth;
                middleArea.setLayoutParams(layoutParams);


            }
        });
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showSnackbar(String message) {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.skyblue))
                .setTextColor(Color.BLACK);
        snackbar.show();
    }
}
