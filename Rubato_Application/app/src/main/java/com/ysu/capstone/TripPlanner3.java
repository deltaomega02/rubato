package com.ysu.capstone;

// Android 기본 UI 컴포넌트
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.MotionEvent;

// Android 레이아웃 및 디자인
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;

// Android 기본 기능
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.DisplayMetrics;

// AndroidX 지원 라이브러리
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

// Naver Maps 관련
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;

// 캘린더 관련
import com.prolificinteractive.materialcalendarview.CalendarDay;

// 데이터 처리 및 유틸리티
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 네트워크 통신
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.ysu.capstone.TripDataCache;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RetrofitClient;
import com.ysu.capstone.network.RouteSaveRequest;
import com.ysu.capstone.network.RouteSaveResponse;

public class TripPlanner3 extends AppCompatActivity implements OnMapReadyCallback {

    // UI 컴포넌트 - 기본 뷰
    private ImageView back;
    private TextView travelDate;
    private TextView placeD;
    private TextView dayText;
    private TextView dayNumText;
    private TextView dateText;
    private ImageView nextDayButton;
    private ImageView prevDayButton;
    private ImageView addDestinationButton;
    private ImageView save;
    private LinearLayout planContainer;

    // UI 컴포넌트 - 플로팅 레이아웃 관련
    private LinearLayout shapeLayout;
    private TextView questionMark;
    private LinearLayout linearLayout1, linearLayout2;
    private GradientDrawable shapeBackground;
    private View dividerLine;
    private View fullScreenLayout;
    private boolean isCircle = true;  // 초기 상태는 원형

    // 네이버 지도 관련
    private NaverMap naverMap;
    private List<Marker> currentMarkers = new ArrayList<>();
    private List<PolylineOverlay> polylines = new ArrayList<>();
    private final LatLng seoulStation = new LatLng(37.5563, 126.9723);
    private final LatLng busanStation = new LatLng(35.1155, 129.0410);
    private final LatLng daejeonStation = new LatLng(36.3320, 127.4346);
    private LatLng[] stations = {seoulStation, busanStation, daejeonStation};

    // 데이터 관련
    private int currentDay = 1;
    private int totalDays = 1;
    private List<Date> selectedDates = new ArrayList<>();
    private List<String> selectedLocations = new ArrayList<>();
    private HashMap<Integer, List<String>> dayWiseDestinations = new HashMap<>();

    // 유틸리티
    private ExecutorService executorService;
    private final OkHttpClient client = new OkHttpClient();
    private static final int REQUEST_CODE_SELECT_PLACE = 1;
    private String apiKeyId;
    private String apiKey;
    private int screenWidth;

    private TripDataCache dataCache = TripDataCache.getInstance();

    private Dialog savingDialog;


    private FloatingActionButton mainFab;
    private LinearLayout expandedMenu;
    private FloatingActionButton askAiFab;
    private FloatingActionButton otherPathsFab;
    private boolean isFabExpanded = false;

    private MapImageManager mapImageManager;
    private Handler handler = new Handler();
    private Runnable checkRouteRunnable;



    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 생명주기 메서드_1 :: 액티비티 초기화 및 뷰 설정하는 메서드
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_planner3);


        // 화면 메트릭스 초기화
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;

        // API 키 및 UI 요소 초기화
        apiKeyId = getResources().getString(R.string.naver_api_key_id);
        apiKey = getResources().getString(R.string.naver_api_key);
        back = findViewById(R.id.back);
        travelDate = findViewById(R.id.travel_date);
        placeD = findViewById(R.id.place_d);
        dayText = findViewById(R.id.day);
        dayNumText = findViewById(R.id.Day_num);
        dateText = findViewById(R.id.date);
        nextDayButton = findViewById(R.id.nextDayButton);
        prevDayButton = findViewById(R.id.prevDayButton);
        planContainer = findViewById(R.id.list_layout);
        addDestinationButton = findViewById(R.id.addDestinationButton);

        mainFab = findViewById(R.id.mainFab);
        expandedMenu = findViewById(R.id.expandedMenu);
        askAiFab = findViewById(R.id.askAiFab);
        otherPathsFab = findViewById(R.id.otherPathsFab);

        setupFabClickListeners();


        MotionLayout parentLayout = findViewById(R.id.full_screen);
        fullScreenLayout = findViewById(R.id.full_screen);

        // MapImageManager 초기화
        mapImageManager = new MapImageManager(this);
        //mapImageManager.startPeriodicCleanup(); // 30분마다 자동 정리 시작. 이놈은 일단 보류에요!


        ScrollView scrollView = findViewById(R.id.scroll_layout);
        MotionLayout motionLayout = findViewById(R.id.full_screen);

        final int[] lastScrollY = {0};  // 이전 스크롤 위치 저장
        final boolean[] isExpanded = {false};  // 현재 확장 상태 저장

        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int currentScrollY = scrollView.getScrollY();
                View child = scrollView.getChildAt(0);
                int maxScroll = (child != null) ? (child.getHeight() - scrollView.getHeight()) : 0;

                // 스크롤 방향 확인 (위로 스크롤 시 양수, 아래로 스크롤 시 음수)
                int scrollDelta = currentScrollY - lastScrollY[0];

                if (!isExpanded[0] && scrollDelta > 30) {
                    // 위로 빠르게 스크롤하면 확장
                    motionLayout.transitionToEnd();
                    isExpanded[0] = true;
                } else if (isExpanded[0] && scrollDelta < -50 && currentScrollY == 0) {
                    // 아래로 빠르게 스크롤하고 최상단에 도달하면 축소
                    motionLayout.transitionToStart();
                    isExpanded[0] = false;
                }

                lastScrollY[0] = currentScrollY;
            }
        });

        // ScrollView 내부의 터치 이벤트도 처리
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private static final float DRAG_THRESHOLD = 100;  // 드래그 임계값

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endY = event.getY();
                        float deltaY = endY - startY;

                        if (deltaY > DRAG_THRESHOLD && scrollView.getScrollY() == 0 && isExpanded[0]) {
                            // 아래로 크게 드래그하고 최상단일 때 축소
                            motionLayout.transitionToStart();
                            isExpanded[0] = false;
                            return true;
                        }
                        break;
                }
                return false;
            }
        });

        askAiFab.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RenderEffect blurEffect = RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP);
                fullScreenLayout.setRenderEffect(blurEffect);
            }

            Intent intent = new Intent(TripPlanner3.this, ChatActivity.class);

            // 기본 여행 정보 전달
            intent.putStringArrayListExtra("selectedLocations", new ArrayList<>(selectedLocations));
            intent.putExtra("numOfDays", totalDays);
            intent.putExtra("currentDay", currentDay);

            // 날짜 정보 전달
            ArrayList<String> selectedDatesAsStrings = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            for (Date date : selectedDates) {
                selectedDatesAsStrings.add(dateFormat.format(date));
            }
            intent.putStringArrayListExtra("selectedDates", selectedDatesAsStrings);

            // 현재까지의 장소 정보 전달
            for (int day = 1; day <= totalDays; day++) {
                List<String> places = dayWiseDestinations.get(day);
                if (places != null && !places.isEmpty()) {
                    ArrayList<String> placeNames = new ArrayList<>();
                    ArrayList<String> placeAddresses = new ArrayList<>();
                    ArrayList<String> latitudes = new ArrayList<>();
                    ArrayList<String> longitudes = new ArrayList<>();

                    for (String placeInfo : places) {
                        String[] parts = placeInfo.split(",");
                        if (parts.length >= 4) {
                            placeNames.add(parts[0]);
                            placeAddresses.add(parts[1]);
                            latitudes.add(parts[2]);
                            longitudes.add(parts[3]);
                        }
                    }

                    // 각 날짜별 장소 정보 전달
                    intent.putStringArrayListExtra("placeNames_" + day, placeNames);
                    intent.putStringArrayListExtra("placeAddresses_" + day, placeAddresses);
                    intent.putStringArrayListExtra("latitudes_" + day, latitudes);
                    intent.putStringArrayListExtra("longitudes_" + day, longitudes);
                }
            }

            startActivityForResult(intent, REQUEST_CODE_SELECT_PLACE);
            collapseFabMenu();
        });
        otherPathsFab.setOnClickListener(v -> {
            Intent intent = new Intent(TripPlanner3.this, SharedRouteListActivity.class);
            ArrayList<String> locationsToSend = new ArrayList<>(selectedLocations);
            String travelPeriodToSend = travelDate.getText().toString();
            intent.putStringArrayListExtra("selectedLocations", locationsToSend);
            intent.putExtra("travelPeriod", travelPeriodToSend);
            startActivity(intent);
            collapseFabMenu();
        });

        Intent intent = getIntent();
        if (intent != null) {
            int numOfNights = intent.getIntExtra("numOfNights", 0);
            totalDays = intent.getIntExtra("numOfDays", 1);
            ArrayList<CalendarDay> selectedCalendarDays = intent.getParcelableArrayListExtra("selectedDates");
            selectedLocations = intent.getStringArrayListExtra("selectedLocations");
            ArrayList<String> selectedPlaces = intent.getStringArrayListExtra("selectedPlaces");
            ArrayList<String> selectedPlaceAddresses = intent.getStringArrayListExtra("selectedPlaceAddresses");
            ArrayList<String> placeLatitudes = intent.getStringArrayListExtra("placeLatitudes");
            ArrayList<String> placeLongitudes = intent.getStringArrayListExtra("placeLongitudes");
            int receivedCurrentDay = intent.getIntExtra("currentDay", -1);

            // currentDay 설정
            if (receivedCurrentDay != -1) {
                currentDay = receivedCurrentDay;
//                Log.d("TripPlanner3", "설정된 currentDay: " + currentDay);
            } else {
                currentDay = 1;
//                Log.d("TripPlanner3", "기본 currentDay로 설정: " + currentDay);
            }

            // 거리 데이터 처리
            ArrayList<String> distances = intent.getStringArrayListExtra("distances");
            if (distances != null) {
                dayWiseDistances.put(currentDay, new ArrayList<>(distances));
//                Log.d("TripPlanner3", "현재 날짜: " + currentDay);
//                Log.d("TripPlanner3", "받은 거리 데이터: " + distances.toString());
//                Log.d("TripPlanner3", "저장된 거리 데이터: " + dayWiseDistances.get(currentDay));
            }

            // 장소 데이터 처리
            if (selectedPlaces != null && selectedPlaceAddresses != null) {
//                Log.d("TripPlanner3", "dayWiseDestinations에 데이터 추가 시작 (currentDay: " + currentDay + ")");

                // 새로운 장소들의 리스트 생성
                List<String> newPlaces = new ArrayList<>();

                // 새로운 장소들 추가
                for (int i = 0; i < selectedPlaces.size(); i++) {
                    String placeDetails = selectedPlaces.get(i) + ","
                            + selectedPlaceAddresses.get(i) + ","
                            + placeLatitudes.get(i) + ","
                            + placeLongitudes.get(i);
                    newPlaces.add(placeDetails);
//                    Log.d("TripPlanner3", "새로 추가된 장소 정보: " + placeDetails);
                }

                // 데이터 저장 (기존 데이터를 새 데이터로 완전히 교체)
                dayWiseDestinations.put(currentDay, newPlaces);
                dataCache.setDestinations(currentDay, newPlaces);

                // 거리 데이터도 새로 설정
                if (distances != null && !distances.isEmpty()) {
                    ArrayList<String> newDistances = new ArrayList<>(distances);
                    dayWiseDistances.put(currentDay, newDistances);
                    dataCache.setDistances(currentDay, newDistances);
//
//                    Log.d("TripPlanner3", "현재 날짜: " + currentDay);
//                    Log.d("TripPlanner3", "새로운 거리 데이터: " + newDistances);
                } else {
                    // 거리 데이터가 없는 경우 새로 계산 시작
                    if (newPlaces.size() >= 2) {
                        calculateDistancesForDay(currentDay, newPlaces);
                    }
                }
            }

            // 선택된 날짜 처리
            if (selectedCalendarDays != null) {
                selectedDates.clear();
                for (CalendarDay calendarDay : selectedCalendarDays) {
                    selectedDates.add(calendarDay.getDate());
                }
            }

            // UI 업데이트
            if (selectedLocations != null && !selectedLocations.isEmpty()) {
                placeD.setText(String.join(", ", selectedLocations));
            } else {
                placeD.setText("선택된 지역이 없습니다.");
            }
            travelDate.setText(numOfNights + "박 " + totalDays + "일의 여행");
            updatePlanContainer();
            updateDayContent();

            // 목적지 추가 버튼 리스너
            addDestinationButton.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                boolean placesLoaded = prefs.getBoolean("places_loaded", false);

                if (!placesLoaded) {
                    // 화면 흔들림 효과
                    Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                    findViewById(android.R.id.content).startAnimation(shake);

                    // Snackbar로 메시지 표시
                    Snackbar.make(v, "관광지 정보를 불러오는 중입니다. 잠시만 기다려주세요.", Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(ContextCompat.getColor(this, R.color.skyblue))
                            .setTextColor(Color.WHITE)
                            .show();
                    return;
                }

                Intent tripPlanner1_1Intent = new Intent(TripPlanner3.this, TripPlanner1_1.class);

                tripPlanner1_1Intent.putStringArrayListExtra("selectedLocations", new ArrayList<>(selectedLocations));
                tripPlanner1_1Intent.putParcelableArrayListExtra("selectedDates", getIntent().getParcelableArrayListExtra("selectedDates"));
                tripPlanner1_1Intent.putExtra("numOfNights", getIntent().getIntExtra("numOfNights", 0));
                tripPlanner1_1Intent.putExtra("numOfDays", getIntent().getIntExtra("numOfDays", 1));
                tripPlanner1_1Intent.putExtra("currentDay", currentDay);

                // 현재 선택된 장소들의 정보 전달
                List<String> currentPlaces = dayWiseDestinations.get(currentDay);
                if (currentPlaces != null && !currentPlaces.isEmpty()) {
                    ArrayList<String> currentSelectedPlaces = new ArrayList<>();
                    ArrayList<String> currentSelectedAddresses = new ArrayList<>();
                    ArrayList<String> currentLatitudes = new ArrayList<>();
                    ArrayList<String> currentLongitudes = new ArrayList<>();

                    for (String placeInfo : currentPlaces) {
                        String[] parts = placeInfo.split(",");
                        if (parts.length >= 4) {
                            currentSelectedPlaces.add(parts[0]);
                            currentSelectedAddresses.add(parts[1]);
                            currentLatitudes.add(parts[2]);
                            currentLongitudes.add(parts[3]);
                        }
                    }

                    tripPlanner1_1Intent.putStringArrayListExtra("previouslySelectedPlaces", currentSelectedPlaces);
                    tripPlanner1_1Intent.putStringArrayListExtra("previouslySelectedAddresses", currentSelectedAddresses);
                    tripPlanner1_1Intent.putStringArrayListExtra("previouslySelectedLatitudes", currentLatitudes);
                    tripPlanner1_1Intent.putStringArrayListExtra("previouslySelectedLongitudes", currentLongitudes);
                }

//                Log.d("TripPlanner3", "TripPlanner1_1로 전달하는 currentDay: " + currentDay);
                startActivityForResult(tripPlanner1_1Intent, REQUEST_CODE_SELECT_PLACE);
            });

            save = findViewById(R.id.ic_save);
            save.setOnClickListener(v -> checkAndShowSavingProgress());
        }

        // 날짜 이동 버튼 리스너
        nextDayButton.setOnClickListener(v -> {
            if (currentDay < totalDays) {
                currentDay++;
                updateDayContent();
            }
        });

        prevDayButton.setOnClickListener(v -> {
            if (currentDay > 1) {
                currentDay--;
                updateDayContent();
            }
        });

        // 뒤로가기 버튼 리스너
        back.setOnClickListener(view -> {
//            if (!isCircle) {
//                animateToCircle();
//                isCircle = true;
//            }

            Intent backIntent = new Intent(TripPlanner3.this, TripPlanner2.class);
            backIntent.putStringArrayListExtra("selectedLocations", new ArrayList<>(selectedLocations));
            backIntent.putParcelableArrayListExtra("selectedDates", getIntent().getParcelableArrayListExtra("selectedDates"));
            backIntent.putExtra("numOfNights", getIntent().getIntExtra("numOfNights", 0));
            backIntent.putExtra("numOfDays", getIntent().getIntExtra("numOfDays", 1));
            startActivity(backIntent);
            finish();

        });

        checkRouteRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                String routeStatus = prefs.getString("route_generation_status", null);
                String routeSchedule = prefs.getString("auto_route_schedule", null);

//                // 로그 추가
//                Log.d("TripPlanner3", "Checking route status: " + routeStatus);
//                if (routeSchedule != null) {
//                    Log.d("TripPlanner3", "Route schedule exists: " + routeSchedule);
//                }

                if (routeStatus != null && routeSchedule != null) {
                    // 경로가 생성되었을 때 처리
                    Log.d("TripPlanner3", "Starting to process auto route");
                    processAutoRoute(routeStatus, routeSchedule);
                    // 경로를 찾았으니 더 이상 체크하지 않음
                    handler.removeCallbacks(this);
//                    Log.d("TripPlanner3", "Route processing completed, stopped checking");
                } else {
                    // 1초마다 다시 확인
                    handler.postDelayed(this, 1000);
//                    Log.d("TripPlanner3", "No route data yet, will check again in 1 second");
                }
            }
        };
        handler.post(checkRouteRunnable);
        // 네이버 지도 초기화
        initNaverMap();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRouteRunnable != null) {
            handler.removeCallbacks(checkRouteRunnable);
        }
    }

    private void processAutoRoute(String status, String schedule) {
        try {
            // 기존 감시 중단
            if (handler != null && checkRouteRunnable != null) {
                handler.removeCallbacks(checkRouteRunnable);
                Log.d("TripPlanner3", "Route checking stopped");
            }

            SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
            prefs.edit()
                    .remove("route_generation_status")
                    .remove("auto_route_schedule")
                    .apply();

            JSONObject scheduleJson = new JSONObject(schedule);
            Iterator<String> keys = scheduleJson.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                int day = Integer.parseInt(key);
                JSONArray placesArray = scheduleJson.getJSONArray(key);
                List<String> dayPlaces = new ArrayList<>();

                Log.d("TripPlanner3", "Processing day " + day);

                for (int i = 0; i < placesArray.length(); i++) {
                    JSONObject placeObject = placesArray.getJSONObject(i);
                    String placeInfo = String.format("%s,%s,%s,%s",
                            placeObject.getString("name"),
                            placeObject.getString("address"),
                            placeObject.getString("latitude"),
                            placeObject.getString("longitude")
                    );
                    dayPlaces.add(placeInfo);
                    Log.d("TripPlanner3", "Added place: " + placeInfo);
                }

                dayWiseDestinations.put(day, dayPlaces);
                dataCache.setDestinations(day, dayPlaces);

                // 거리 계산
                if (dayPlaces.size() > 1) {
                    ArrayList<String> distances = new ArrayList<>();
                    for (int i = 0; i < dayPlaces.size() - 1; i++) {
                        String[] current = dayPlaces.get(i).split(",");
                        String[] next = dayPlaces.get(i + 1).split(",");

                        if (current.length >= 4 && next.length >= 4) {
                            calculateSingleDistance(
                                    day, i,
                                    current[2], current[3],
                                    next[2], next[3]
                            );
                        }
                        distances.add(""); // 거리 데이터 초기화
                    }
                    dayWiseDistances.put(day, distances);
                    dataCache.setDistances(day, distances);
                }
            }

            // UI 업데이트
            runOnUiThread(() -> {
                updateDayContent();
                Log.d("TripPlanner3", "UI updated with new route data");
            });

        } catch (JSONException e) {
            Log.e("TripPlanner3", "JSON parsing error: " + e.getMessage(), e);
        }
    }


    // 생명주기 메서드_2 :: 액티비티 결과 처리하는 메서드 (채팅 화면 복귀, 장소 선택 결과)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_PLACE && resultCode == RESULT_OK && data != null) {
            if (data.hasExtra("schedule")) {
                try {
                    JSONObject scheduleJson = new JSONObject(data.getStringExtra("schedule"));
                    JSONObject schedule = scheduleJson.getJSONObject("schedule");
                    Iterator<String> keys = schedule.keys();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        int day = Integer.parseInt(key);
                        JSONArray placesArray = schedule.getJSONArray(key);
                        List<String> dayPlaces = new ArrayList<>();

                        // 해당 일차의 기존 데이터를 완전히 새로운 데이터로 교체
                        for (int i = 0; i < placesArray.length(); i++) {
                            JSONObject placeObject = placesArray.getJSONObject(i);
                            String placeInfo = String.format("%s,%s,%s,%s",
                                    placeObject.getString("name"),
                                    placeObject.getString("address"),
                                    placeObject.getString("latitude"),
                                    placeObject.getString("longitude")
                            );
                            dayPlaces.add(placeInfo);
                        }

                        // 해당 일차의 데이터 완전히 교체
                        dayWiseDestinations.put(day, dayPlaces);
                        dataCache.setDestinations(day, dayPlaces);

                        // 거리 데이터도 초기화
                        dayWiseDistances.put(day, new ArrayList<>());
                        dataCache.setDistances(day, new ArrayList<>());

                        // 새로운 거리 계산 시작
                        if (dayPlaces.size() > 1) {
                            ArrayList<String> distances = new ArrayList<>();
                            for (int i = 0; i < dayPlaces.size() - 1; i++) {
                                String[] current = dayPlaces.get(i).split(",");
                                String[] next = dayPlaces.get(i + 1).split(",");

                                String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start="
                                        + current[3] + "," + current[2] + "&goal=" + next[3] + "," + next[2];

                                Request request = new Request.Builder()
                                        .url(url)
                                        .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                                        .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                                        .build();

                                final int index = i;
                                final int finalDay = day;
                                new OkHttpClient().newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        runOnUiThread(() -> {
                                            ArrayList<String> currentDistances = dayWiseDistances.get(finalDay);
                                            if (currentDistances == null) {
                                                currentDistances = new ArrayList<>();
                                                dayWiseDistances.put(finalDay, currentDistances);
                                            }
                                            while (currentDistances.size() <= index) {
                                                currentDistances.add("");
                                            }
                                            currentDistances.set(index, "계산 실패");
                                            dataCache.setDistances(finalDay, currentDistances);
                                            updatePlanContainer();
                                        });
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        if (response.isSuccessful()) {
                                            String responseData = response.body().string();
                                            try {
                                                JSONObject jsonObject = new JSONObject(responseData);
                                                JSONObject route = jsonObject.getJSONObject("route")
                                                        .getJSONArray("traoptimal")
                                                        .getJSONObject(0);
                                                double distance = route.getJSONObject("summary").getInt("distance") / 1000.0;
                                                String distanceText = String.format("%.1f km", distance);

                                                runOnUiThread(() -> {
                                                    ArrayList<String> currentDistances = dayWiseDistances.get(finalDay);
                                                    if (currentDistances == null) {
                                                        currentDistances = new ArrayList<>();
                                                        dayWiseDistances.put(finalDay, currentDistances);
                                                    }
                                                    while (currentDistances.size() <= index) {
                                                        currentDistances.add("");
                                                    }
                                                    currentDistances.set(index, distanceText);
                                                    dataCache.setDistances(finalDay, currentDistances);
                                                    updatePlanContainer();
                                                });
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }

                    updateDayContent();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "일정 데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                fullScreenLayout.setRenderEffect(null);
            }
        }
    }


    // 생명주기 메서드_3 :: 뒤로가기 버튼 처리하는 메서드 (블러 효과 제거, 애니메이션 처리)
    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            View viewToBlur = findViewById(R.id.full_screen);
            viewToBlur.setRenderEffect(null);
        }

        super.onBackPressed();

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

//        if (!isCircle) {
//            animateToCircle();
//            isCircle = true;
//        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private final int[] markerColors = {
            0xFFB5E6B5,  // Pastel Green
            0xFFB5D8FF,  // Pastel Blue
            0xFFFFB5D8,  // Pastel Pink
            0xFFE1B5FF,  // Pastel Purple
            0xFFFFD6B5,  // Pastel Orange
            0xFFB5F4FF   // Pastel Cyan
    };

    private Map<Marker, Boolean> markerExpandStates = new HashMap<>();
    private Map<Marker, String> markerFullNames = new HashMap<>();

    // 네이버 지도 관련 메서드_1 :: 네이버 지도 초기화 및 프래그먼트 설정하는 메서드
    private void initNaverMap() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.Map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.Map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    // 네이버 지도 관련 메서드_2 :: 지도가 준비되었을 때 기본 설정(위치 추적, 줌, 카메라) 하는 메서드
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(true);

        // 현재 날짜의 장소 데이터 확인
        List<String> placesForCurrentDay = dayWiseDestinations.get(currentDay);

        if (placesForCurrentDay != null && !placesForCurrentDay.isEmpty()) {
            // 장소 데이터가 있으면 updateDayContent() 호출만 하고 종료
            updateDayContent();
        }
        else if (selectedLocations != null && !selectedLocations.isEmpty()) {
            // 장소 데이터가 없을 때만 시청/역/터미널 검색
            String mainLocation = selectedLocations.get(0);
            String url = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode"
                    + "?query=" + Uri.encode(mainLocation + "시청");

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                    .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    searchWithSuffix(mainLocation, new String[]{"역", "터미널"}, 0, naverMap);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray addresses = jsonObject.getJSONArray("addresses");

                            if (addresses.length() > 0) {
                                JSONObject firstResult = addresses.getJSONObject(0);
                                double latitude = Double.parseDouble(firstResult.getString("y"));
                                double longitude = Double.parseDouble(firstResult.getString("x"));

                                LatLng defaultLocation = new LatLng(latitude, longitude);
                                for (int i = 1; i <= totalDays; i++) {
                                    if (dayWiseDestinations.get(i) == null || dayWiseDestinations.get(i).isEmpty()) {
                                        List<String> defaultPlace = new ArrayList<>();
                                        defaultPlace.add(mainLocation + "시청," + mainLocation + "," + latitude + "," + longitude);
                                        dayWiseDestinations.put(i, defaultPlace);
                                    }
                                }

                                runOnUiThread(() -> {
                                    CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(defaultLocation, 13)
                                            .animate(CameraAnimation.Fly, 1000);
                                    naverMap.moveCamera(cameraUpdate);
                                });
                            } else {
                                searchWithSuffix(mainLocation, new String[]{"역", "터미널"}, 0, naverMap);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            searchWithSuffix(mainLocation, new String[]{"역", "터미널"}, 0, naverMap);
                        }
                    }
                }
            });
        } else {
            // 선택된 지역이 없는 경우 서울 중심부로 설정
            LatLng seoulCenter = new LatLng(37.5666, 126.9784);
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(seoulCenter, 11);
            naverMap.moveCamera(cameraUpdate);
        }

        Log.d("TripPlanner3", "네이버 지도 초기화 완료");
    }


    private void searchLocationSequentially(String location, NaverMap naverMap) {
        // 검색할 키워드 순서 정의: 공항 -> 역 -> 터미널
        String[] searchSuffixes = {"공항", "역", "터미널"};
        searchWithSuffix(location, searchSuffixes, 0, naverMap);
    }

    private void searchWithSuffix(String location, String[] suffixes, int index, NaverMap naverMap) {
        // 모든 접미사 시도 후에도 실패한 경우
        if (index >= suffixes.length) {
            // 지역명으로만 마지막 시도
            searchLocation(location, "", naverMap, false);
            return;
        }

        String searchQuery = location + suffixes[index];
        searchLocation(location, suffixes[index], naverMap, true);
    }

    private void searchLocation(String baseLocation, String suffix, NaverMap naverMap, boolean tryNext) {
        String searchQuery = suffix.isEmpty() ? baseLocation : baseLocation + suffix;
        String url = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode"
                + "?query=" + Uri.encode(searchQuery);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (tryNext) {
                    // 다음 접미사로 시도
                    searchWithSuffix(baseLocation, new String[]{"공항", "역", "터미널"},
                            getSuffixIndex(suffix) + 1, naverMap);
                } else {
                    // 모든 시도 실패시 서울 중심부로 설정
                    runOnUiThread(() -> {
                        LatLng seoulCenter = new LatLng(37.5666, 126.9784);
                        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(seoulCenter, 11);
                        naverMap.moveCamera(cameraUpdate);
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray addresses = jsonObject.getJSONArray("addresses");

                        if (addresses.length() > 0) {
                            JSONObject firstResult = addresses.getJSONObject(0);
                            double latitude = Double.parseDouble(firstResult.getString("y"));
                            double longitude = Double.parseDouble(firstResult.getString("x"));

                            runOnUiThread(() -> {
                                LatLng location = new LatLng(latitude, longitude);
                                // 검색 결과에 따른 줌 레벨 조정
                                int zoomLevel;
                                if (suffix.equals("공항")) {
                                    zoomLevel = 13;  // 공항은 넓은 영역
                                } else if (suffix.equals("역")) {
                                    zoomLevel = 14;  // 역은 중간 영역
                                } else if (suffix.equals("터미널")) {
                                    zoomLevel = 14;  // 터미널도 중간 영역
                                } else {
                                    // 지역 규모에 따른 줌 레벨
                                    zoomLevel = baseLocation.contains("도") ? 9 :
                                            (baseLocation.equals("제주") || baseLocation.equals("제주도")) ? 10 :
                                                    baseLocation.matches(".*(시|군|구)") ? 12 : 11;
                                }

                                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(location, zoomLevel)
                                        .animate(CameraAnimation.Fly, 1000);
                                naverMap.moveCamera(cameraUpdate);

                                Log.d("TripPlanner3", "위치 찾음: " + searchQuery +
                                        " (위도: " + latitude + ", 경도: " + longitude + ")");
                            });
                        } else if (tryNext) {
                            // 결과가 없으면 다음 접미사로 시도
                            searchWithSuffix(baseLocation, new String[]{"공항", "역", "터미널"},
                                    getSuffixIndex(suffix) + 1, naverMap);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (tryNext) {
                            searchWithSuffix(baseLocation, new String[]{"공항", "역", "터미널"},
                                    getSuffixIndex(suffix) + 1, naverMap);
                        }
                    }
                }
            }
        });
    }

    private int getSuffixIndex(String suffix) {
        switch (suffix) {
            case "공항": return 0;
            case "역": return 1;
            case "터미널": return 2;
            default: return -1;
        }
    }

    // 네이버 지도 관련 메서드_3 :: 단일 위치에 대한 카메라 이동과 마커 설정하는 메서드
    private void showSingleLocation(LatLng location, String placeName) {
        if (naverMap == null) {
            Log.e("TripPlanner3", "naverMap이 null입니다");
            return;
        }

        // 기존 마커와 폴리라인 제거
        for (Marker marker : currentMarkers) {
            marker.setMap(null);
        }
        currentMarkers.clear();

        for (PolylineOverlay polyline : polylines) {
            polyline.setMap(null);
        }
        polylines.clear();

        // 새로운 마커 생성
        Bitmap markerBitmap = createCustomMarkerBitmap(placeName, 1, false);
        Marker marker = new Marker();
        marker.setPosition(location);
        marker.setIcon(OverlayImage.fromBitmap(markerBitmap));
        marker.setMap(naverMap);

        // 마커 상태 저장
        markerExpandStates.put(marker, false);
        markerFullNames.put(marker, placeName);

        // 클릭 이벤트 설정
        marker.setOnClickListener(overlay -> {
            Marker clickedMarker = (Marker) overlay;
            boolean isExpanded = markerExpandStates.get(clickedMarker);
            String fullName = markerFullNames.get(clickedMarker);

            // 상태 토글
            markerExpandStates.put(clickedMarker, !isExpanded);

            // 마커 업데이트
            clickedMarker.setIcon(
                    OverlayImage.fromBitmap(
                            createCustomMarkerBitmap(fullName, 1, !isExpanded)
                    )
            );

            return true;
        });

        currentMarkers.add(marker);

        // 카메라 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(location, 15);  // 줌 레벨은 적절히 조정
        naverMap.moveCamera(cameraUpdate);
    }

    // 네이버 지도 관련 메서드_4 :: 여러 위치에 대한 마커, 경로, 카메라 설정하는 메서드
    private void updateMapWithLocation(List<LatLng> locations, List<String> placeNames, boolean isDefaultLocation) {
        if (naverMap == null) {
            Log.e("TripPlanner3", "naverMap이 null입니다");
            return;
        }

        // 단일 위치인 경우
        if (locations.size() == 1 && !isDefaultLocation) {
            showSingleLocation(locations.get(0), placeNames.get(0));
            return;
        }

        // 기존 마커와 폴리라인 제거
        for (Marker marker : currentMarkers) {
            marker.setMap(null);
        }
        currentMarkers.clear();

        for (PolylineOverlay polyline : polylines) {
            polyline.setMap(null);
        }
        polylines.clear();

        // 장소가 있는 경우에만 마커 추가 및 경로 표시
        if (!locations.isEmpty() && !isDefaultLocation) {
            for (int i = 0; i < locations.size(); i++) {
                LatLng location = locations.get(i);
                String placeName = placeNames.get(i);
                int placeNumber = i + 1;

                // 커스텀 마커 아이콘 생성
                Bitmap markerBitmap = createCustomMarkerBitmap(placeName, placeNumber, false);

                // 마커 생성 및 설정
                Marker marker = new Marker();
                marker.setPosition(location);
                marker.setIcon(OverlayImage.fromBitmap(markerBitmap));
                marker.setMap(naverMap);

                // 마커 상태 저장
                markerExpandStates.put(marker, false);
                markerFullNames.put(marker, placeName);

                // 클릭 이벤트 설정
                final int finalI = i;
                marker.setOnClickListener(overlay -> {
                    Marker clickedMarker = (Marker) overlay;
                    boolean isExpanded = markerExpandStates.get(clickedMarker);
                    String fullName = markerFullNames.get(clickedMarker);

                    // 상태 토글
                    markerExpandStates.put(clickedMarker, !isExpanded);

                    // 마커 업데이트
                    clickedMarker.setIcon(
                            OverlayImage.fromBitmap(
                                    createCustomMarkerBitmap(fullName, finalI + 1, !isExpanded)
                            )
                    );

                    return true;
                });

                currentMarkers.add(marker);

                // 마지막 위치가 아닌 경우에만 경로 요청
                if (i < locations.size() - 1) {
                    fetchRoutePathWithColor(locations.get(i), locations.get(i + 1), i);
                }
            }

            // 모든 마커를 포함하는 카메라 위치로 이동
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng location : locations) {
                boundsBuilder.include(location);
            }
            LatLngBounds bounds = boundsBuilder.build();
            CameraUpdate cameraUpdate = CameraUpdate.fitBounds(bounds, 100);
            naverMap.moveCamera(cameraUpdate);
        } else if (isDefaultLocation) {
            // 기본 위치로 넓은 줌 설정
            LatLng defaultLocation = locations.get(0);
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(defaultLocation, 10);
            naverMap.moveCamera(cameraUpdate);
        }
    }

    // 네이버 지도 관련 메서드_5 :: 색상이 있는 경로를 요청하는 메서드
    private void fetchRoutePathWithColor(LatLng start, LatLng end, int segmentIndex) {
        // 좌표를 직접 사용하여 경로 요청
        String url = String.format(Locale.US,
                "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving" +
                        "?start=%.6f,%.6f" +
                        "&goal=%.6f,%.6f" +
                        "&option=traoptimal",
                start.longitude, start.latitude,
                end.longitude, end.latitude);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // 경로 찾기 실패 시 직선으로 연결
                runOnUiThread(() -> drawFallbackRoute(start, end, segmentIndex));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray route = jsonObject
                                .getJSONObject("route")
                                .getJSONArray("traoptimal")
                                .getJSONObject(0)
                                .getJSONArray("path");

                        List<LatLng> pathCoords = new ArrayList<>();
                        for (int i = 0; i < route.length(); i++) {
                            JSONArray point = route.getJSONArray(i);
                            double longitude = point.getDouble(0);
                            double latitude = point.getDouble(1);
                            pathCoords.add(new LatLng(latitude, longitude));
                        }

                        runOnUiThread(() -> drawPolylineWithColor(pathCoords, segmentIndex));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // JSON 파싱 실패 시 직선으로 연결
                        runOnUiThread(() -> drawFallbackRoute(start, end, segmentIndex));
                    }
                } else {
                    // API 응답 실패 시 직선으로 연결
                    runOnUiThread(() -> drawFallbackRoute(start, end, segmentIndex));
                }
            }
        });
    }

    // 경로 찾기 실패 시 사용할 대체 경로 그리기 메소드
    private void drawFallbackRoute(LatLng start, LatLng end, int segmentIndex) {
        List<LatLng> fallbackPath = new ArrayList<>();
        fallbackPath.add(start);

        // 중간 지점 추가 (부드러운 곡선 효과를 위해)
        LatLng midPoint = new LatLng(
                (start.latitude + end.latitude) / 2,
                (start.longitude + end.longitude) / 2
        );
        // 중간 지점을 약간 휘어지게 조정
        double offset = 0.001; // 약 100m 정도의 오프셋
        midPoint = new LatLng(
                midPoint.latitude + offset,
                midPoint.longitude + offset
        );
        fallbackPath.add(midPoint);
        fallbackPath.add(end);

        drawPolylineWithColor(fallbackPath, segmentIndex);
    }



    // 네이버 지도 관련 메서드_6 :: 색상이 있는 경로선을 그리는 메서드
    private void drawPolylineWithColor(List<LatLng> pathCoords, int segmentIndex) {
        // 출발지 마커의 색상을 사용
        int polylineColor = markerColors[segmentIndex % markerColors.length];

        // 외곽선 효과 (더 얇게)
        PolylineOverlay outlinePolyline = new PolylineOverlay();
        outlinePolyline.setCoords(pathCoords);
        outlinePolyline.setColor(0xFFFFFFFF); // 흰색
        outlinePolyline.setWidth(20);
        outlinePolyline.setCapType(PolylineOverlay.LineCap.Round);
        outlinePolyline.setJoinType(PolylineOverlay.LineJoin.Round);
        outlinePolyline.setMap(naverMap);

        // 메인 경로
        PolylineOverlay mainPolyline = new PolylineOverlay();
        mainPolyline.setCoords(pathCoords);
        mainPolyline.setColor(polylineColor);
        mainPolyline.setWidth(16);
        mainPolyline.setCapType(PolylineOverlay.LineCap.Round);
        mainPolyline.setJoinType(PolylineOverlay.LineJoin.Round);
        mainPolyline.setMap(naverMap);

        polylines.add(outlinePolyline);
        polylines.add(mainPolyline);
    }

    // 네이버 지도 관련 메서드_7 :: 장소명 전처리
    private String formatPlaceName(String placeName) {
        // 괄호가 있는 경우 괄호와 그 안의 내용 제거
        int bracketIndex = placeName.indexOf("(");
        if (bracketIndex != -1) {
            return placeName.substring(0, bracketIndex).trim();
        }
        return placeName;
    }

    // 네이버 지도 관련 메서드_7 :: 장소명과 번호를 포함한 커스텀 마커 비트맵을 생성하는 메서드
    private Bitmap createCustomMarkerBitmap(String placeName, int number, boolean isExpanded) {
        View markerView = LayoutInflater.from(this).inflate(R.layout.custom_marker_layout, null);

        TextView numberView = markerView.findViewById(R.id.marker_number);
        TextView placeNameView = markerView.findViewById(R.id.marker_place_name);
        CardView cardView = markerView.findViewById(R.id.marker_card);

        // 마커 색상 설정
        int markerColor = markerColors[(number - 1) % markerColors.length];
        if (cardView != null) {
            cardView.setCardBackgroundColor(markerColor);
        }

        // 숫자는 항상 표시
        numberView.setTextColor(0xFF000000);
        numberView.setText(String.valueOf(number));

        // 장소 이름은 확장 상태일 때만 표시
        placeNameView.setText(placeName);
        placeNameView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // 비트맵 생성
        markerView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                markerView.getMeasuredWidth(),
                markerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return bitmap;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // UI 업데이트 메서드_1 :: 현재 선택된 날짜의 정보와 지도를 업데이트하는 메서드
    private void updateDayContent() {
        // 기본 UI 업데이트
        dayNumText.setText(String.valueOf(currentDay));
        Date currentDate = selectedDates.get(currentDay - 1);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM.dd", Locale.KOREA);
        String formattedDate = dateFormatter.format(currentDate);
        dateText.setText(formattedDate);

        // 데이터 동기화 (캐시와 메모리)
        List<String> placesForCurrentDay = dayWiseDestinations.get(currentDay);
        List<String> cachedPlaces = dataCache.getDestinations(currentDay);

        if (cachedPlaces != null && !cachedPlaces.isEmpty()) {
            // 캐시의 데이터가 더 최신이면 메모리 업데이트
            placesForCurrentDay = new ArrayList<>(cachedPlaces);
            dayWiseDestinations.put(currentDay, placesForCurrentDay);
        } else if (placesForCurrentDay != null && !placesForCurrentDay.isEmpty()) {
            // 메모리의 데이터가 있으면 캐시 업데이트
            dataCache.setDestinations(currentDay, placesForCurrentDay);
        }

        ArrayList<String> distancesForCurrentDay = dayWiseDistances.get(currentDay);
        ArrayList<String> cachedDistances = dataCache.getDistances(currentDay);

        if (cachedDistances != null && !cachedDistances.isEmpty()) {
            distancesForCurrentDay = new ArrayList<>(cachedDistances);
            dayWiseDistances.put(currentDay, distancesForCurrentDay);
        } else if (distancesForCurrentDay != null) {
            dataCache.setDistances(currentDay, distancesForCurrentDay);
        }

        // 내비게이션 버튼 상태 업데이트
        prevDayButton.setVisibility(currentDay == 1 ? View.GONE : View.VISIBLE);
        nextDayButton.setVisibility(currentDay == totalDays ? View.GONE : View.VISIBLE);

        // 날짜 텍스트 업데이트
        if (currentDay == 1) {
            dayText.setText("첫날");
        } else if (currentDay == totalDays) {
            dayText.setText("마지막날");
        } else {
            dayText.setText(DateHelper.getKoreanDayString(currentDay));
        }

        // 기존 마커와 경로 모두 제거
        for (Marker marker : currentMarkers) {
            marker.setMap(null);
        }
        currentMarkers.clear();

        for (PolylineOverlay polyline : polylines) {
            polyline.setMap(null);
        }
        polylines.clear();

        // 지도 업데이트를 위한 데이터 준비
        List<LatLng> locations = new ArrayList<>();
        List<String> placeNames = new ArrayList<>();

        Log.d("TripPlanner3", "현재 날짜(" + currentDay + ")의 장소 목록: " +
                (placesForCurrentDay != null ? placesForCurrentDay.toString() : "null"));

        if (placesForCurrentDay != null && !placesForCurrentDay.isEmpty()) {
            for (String placeInfo : placesForCurrentDay) {
                String[] parts = placeInfo.split(",");
                if (parts.length >= 4) {
                    String placeName = parts[0];
                    Double latitude = null;
                    Double longitude = null;

                    // 위도, 경도가 2번째와 3번째에 있는지 확인
                    if (isNumeric(parts[2]) && isNumeric(parts[3])) {
                        latitude = Double.parseDouble(parts[2]);
                        longitude = Double.parseDouble(parts[3]);
                    }
                    // 위도, 경도가 3번째와 4번째에 밀려있는 경우 처리
                    else if (parts.length >= 5 && isNumeric(parts[3]) && isNumeric(parts[4])) {
                        latitude = Double.parseDouble(parts[3]);
                        longitude = Double.parseDouble(parts[4]);
                    }

                    if (latitude != null && longitude != null) {
//                        Log.d("TripPlanner3", String.format(
//                                "지도에 표시할 장소:\n이름: %s\n위도: %f\n경도: %f",
//                                placeName, latitude, longitude
//                        ));

                        locations.add(new LatLng(latitude, longitude));
                        placeNames.add(placeName);
                    } else {
//                        Log.e("TripPlanner3", "유효하지 않은 데이터: " + placeInfo);
                    }
                }
            }
            updateMapWithLocation(locations, placeNames, false);
        } else {
            // 선택된 지역이 있는 경우 검색 시작
            if (selectedLocations != null && !selectedLocations.isEmpty() && naverMap != null) {
                String mainLocation = selectedLocations.get(0);
                searchLocationSequentially(mainLocation, naverMap);
            } else {
                // 선택된 지역이 없는 경우 서울 중심부로 설정
                if (naverMap != null) {
                    LatLng seoulCenter = new LatLng(37.5666, 126.9784);
                    CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(seoulCenter, 11);
                    naverMap.moveCamera(cameraUpdate);
                }
            }
        }

        updatePlanContainer();
    }


    private void calculateDistancesForDay(int day, List<String> places) {
        if (places.size() < 2) return;

        // 거리 데이터가 이미 있는지 확인
        ArrayList<String> distances = dayWiseDistances.getOrDefault(day, new ArrayList<>());
        boolean needsCalculation = distances.isEmpty();

        // 거리 데이터 초기화
        if (needsCalculation) {
            distances = new ArrayList<>();
            for (int i = 0; i < places.size() - 1; i++) {
                distances.add("");  // 빈 문자열로 초기화
            }
            dayWiseDistances.put(day, distances);
        }

        // UI 업데이트
        updatePlanContainer();

        if (needsCalculation) {
            // 모든 지점 간의 거리를 동시에 계산
            for (int i = 0; i < places.size() - 1; i++) {
                String[] current = places.get(i).split(",");
                String[] next = places.get(i + 1).split(",");

                if (current.length < 4 || next.length < 4) continue;

                String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start="
                        + current[3] + "," + current[2] + "&goal=" + next[3] + "," + next[2];

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                        .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                        .build();

                final int index = i;
                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            ArrayList<String> currentDistances = dayWiseDistances.get(day);
                            if (currentDistances != null && index < currentDistances.size()) {
                                currentDistances.set(index, "계산 실패");
                                dayWiseDistances.put(day, currentDistances);
                                dataCache.setDistances(day, currentDistances);
                                updatePlanContainer();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            try {
                                JSONObject jsonObject = new JSONObject(responseData);
                                JSONObject route = jsonObject.getJSONObject("route")
                                        .getJSONArray("traoptimal")
                                        .getJSONObject(0);
                                double distance = route.getJSONObject("summary").getInt("distance") / 1000.0;
                                String distanceText = String.format("%.1f km", distance);

                                runOnUiThread(() -> {
                                    ArrayList<String> currentDistances = dayWiseDistances.get(day);
                                    if (currentDistances != null && index < currentDistances.size()) {
                                        currentDistances.set(index, distanceText);
                                        dayWiseDistances.put(day, currentDistances);
                                        dataCache.setDistances(day, currentDistances);
                                        updatePlanContainer();
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    ArrayList<String> currentDistances = dayWiseDistances.get(day);
                                    if (currentDistances != null && index < currentDistances.size()) {
                                        currentDistances.set(index, "계산 실패");
                                        dayWiseDistances.put(day, currentDistances);
                                        dataCache.setDistances(day, currentDistances);
                                        updatePlanContainer();
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }
    }

    // UI 업데이트 메서드_2 :: 현재 날짜의 여행지 리스트를 화면에 표시하는 메서드
    private void updatePlanContainer() {
        planContainer.removeAllViews();

        // 자동 생성 경로 상태 확인
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        String routeStatus = prefs.getString("route_generation_status", null);

        // 현재 날짜의 장소 데이터 가져오기
        List<String> placesForCurrentDay = dayWiseDestinations.get(currentDay);
        ArrayList<String> distancesForCurrentDay = dayWiseDistances.get(currentDay);

        // 데이터가 없는 경우 상태에 따라 다른 UI 표시
        if (placesForCurrentDay == null || placesForCurrentDay.isEmpty()) {
            View emptyView;
            if (routeStatus != null) {
                // 자동 일정 생성 중인 경우
                emptyView = LayoutInflater.from(this).inflate(R.layout.loading_layout, planContainer, false);
                ProgressBar progressBar = emptyView.findViewById(R.id.progressBar);
                TextView loadingText = emptyView.findViewById(R.id.loadingText);
                TextView loadingSubText = emptyView.findViewById(R.id.loadingSubText);

                // 메인 텍스트 설정
                loadingText.setText("일정을 생성하고 있습니다");

                // TripPlanner2에서 전달받은 데이터 가져오기
                Intent intent = getIntent();
                ArrayList<String> selectedTags = intent.getStringArrayListExtra("selectedTags");
                String location = selectedLocations != null && !selectedLocations.isEmpty() ?
                        selectedLocations.get(0) : "선택한 지역";
                int numOfNights = intent.getIntExtra("numOfNights", 0);

                // 동적 메시지 생성
                String[] loadingMessages = new String[] {
                        String.format("%s에서의 %d박 %d일 여행\n#%s 태그로 특별한 일정을 만들고 있어요",
                                location, numOfNights, totalDays,
                                selectedTags != null ? String.join(" #", selectedTags) : "여행"),
                        String.format("%s의 숨은 명소들을\n당신의 취향에 맞게 찾고 있어요", location),
                        String.format("여행 전문가가 %s의\n새로운 매력을 찾아내는 중이에요", location),
                        String.format("%d일 동안의 특별한 순간들을\n하나하나 채워넣고 있어요", totalDays),
                        String.format("당신만을 위한 %s 여행\n최적의 동선을 구성하고 있어요", location)
                };

                final int[] messageIndex = {0};
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (emptyView.getParent() != null) {  // 뷰가 아직 표시되어 있는지 확인
                            // 페이드 인/아웃 애니메이션
                            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                            fadeIn.setDuration(1000);
                            fadeIn.setFillAfter(true);

                            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                            fadeOut.setDuration(1000);
                            fadeOut.setStartOffset(3000);
                            fadeOut.setFillAfter(true);

                            // 메시지 업데이트
                            loadingSubText.setText(loadingMessages[messageIndex[0]]);
                            messageIndex[0] = (messageIndex[0] + 1) % loadingMessages.length;

                            loadingSubText.startAnimation(fadeIn);
                            loadingSubText.startAnimation(fadeOut);

                            handler.postDelayed(this, 4000);
                        }
                    }
                }, 0);
            } else {
                // 일반적으로 일정이 없는 경우
                emptyView = LayoutInflater.from(this).inflate(R.layout.empty_plan_layout, planContainer, false);
                TextView emptyText = emptyView.findViewById(R.id.empty_text);
                emptyText.setText("아직 계획된 일정이 없습니다.\n장소를 추가해주세요.");
            }
            planContainer.addView(emptyView);
            return;
        }

        // 일정이 있는 경우의 처리
        LayoutInflater inflater = LayoutInflater.from(this);

        // 장소 목록 표시
        for (int i = 0; i < placesForCurrentDay.size(); i++) {
            // 장소 뷰 추가
            String placeInfo = placesForCurrentDay.get(i);
            String[] parts = placeInfo.split(",");
            if (parts.length < 4) continue;

            String placeName = parts[0];
            View placeView = inflater.inflate(R.layout.inc_plan_detail, planContainer, false);
            TextView placeNumTextView = placeView.findViewById(R.id.travel_num_text);
            placeNumTextView.setText(String.valueOf(i + 1));
            TextView placeNameTextView = placeView.findViewById(R.id.travel_info_text);
            placeNameTextView.setText(placeName);

            ImageButton xButton = placeView.findViewById(R.id.x_ic_button);
            int finalI = i;
            xButton.setOnClickListener(v -> {
                if (finalI >= 0 && finalI < placesForCurrentDay.size()) {
                    // 장소 제거
                    placesForCurrentDay.remove(finalI);

                    // 거리 데이터도 함께 업데이트
                    ArrayList<String> distances = dayWiseDistances.get(currentDay);
                    if (distances != null) {
                        if (finalI < distances.size()) {
                            distances.remove(finalI);
                        }

                        // 재계산이 필요한 구간 설정
                        if (finalI > 0) {
                            int startIdx = finalI - 1;
                            String[] currentPlace = placesForCurrentDay.get(startIdx).split(",");
                            String[] nextPlace = finalI < placesForCurrentDay.size() ? placesForCurrentDay.get(finalI).split(",") : null;

                            if (currentPlace.length >= 4 && nextPlace != null && nextPlace.length >= 4) {
                                calculateSingleDistance(
                                        currentDay,
                                        startIdx,
                                        currentPlace[2], currentPlace[3],
                                        nextPlace[2], nextPlace[3]
                                );
                            }
                        }
                    }

                    // 캐시 업데이트
                    dataCache.setDestinations(currentDay, placesForCurrentDay);
                    dataCache.setDistances(currentDay, distances);

                    // UI 업데이트
                    updateDayContent();
                }
            });
            planContainer.addView(placeView);

            // 마지막 장소가 아닌 경우에만 거리 표시
            if (i < placesForCurrentDay.size() - 1) {
                View distanceView = inflater.inflate(R.layout.inc_plan_order, planContainer, false);
                TextView distanceTextView = distanceView.findViewById(R.id.order_distance);

                // 거리 정보 표시
                if (distancesForCurrentDay != null && i < distancesForCurrentDay.size()) {
                    String distance = distancesForCurrentDay.get(i);
                    if (!TextUtils.isEmpty(distance)) {
                        distanceTextView.setText(distance);
                    } else {
                        distanceTextView.setText("계산 중...");
                    }
                } else {
                    distanceTextView.setText("계산 중...");
                }

                planContainer.addView(distanceView);
            }
        }
    }
    private void calculateSingleDistance(int day, int index, String startLat, String startLng, String endLat, String endLng) {
        String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start="
                + startLng + "," + startLat + "&goal=" + endLng + "," + endLat;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    ArrayList<String> currentDistances = dayWiseDistances.get(day);
                    if (currentDistances != null && index < currentDistances.size()) {
                        currentDistances.set(index, "계산 실패");
                        dayWiseDistances.put(day, currentDistances);
                        dataCache.setDistances(day, currentDistances);
                        updatePlanContainer();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject route = jsonObject.getJSONObject("route")
                                .getJSONArray("traoptimal")
                                .getJSONObject(0);
                        double distance = route.getJSONObject("summary").getInt("distance") / 1000.0;
                        String distanceText = String.format("%.1f km", distance);

                        runOnUiThread(() -> {
                            ArrayList<String> currentDistances = dayWiseDistances.get(day);
                            if (currentDistances != null && index < currentDistances.size()) {
                                currentDistances.set(index, distanceText);
                                dayWiseDistances.put(day, currentDistances);
                                dataCache.setDistances(day, currentDistances);
                                updatePlanContainer();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            ArrayList<String> currentDistances = dayWiseDistances.get(day);
                            if (currentDistances != null && index < currentDistances.size()) {
                                currentDistances.set(index, "계산 실패");
                                dayWiseDistances.put(day, currentDistances);
                                dataCache.setDistances(day, currentDistances);
                                updatePlanContainer();
                            }
                        });
                    }
                }
            }
        });
    }


    private HashMap<String, DistanceInfo> distanceMap = new HashMap<>();
    private ArrayList<Double> placeLatitudes = new ArrayList<>();
    private ArrayList<Double> placeLongitudes = new ArrayList<>();
    private ArrayList<String> selectedPlaces = new ArrayList<>();
    private ArrayList<String> selectedAddresses = new ArrayList<>();
    private HashMap<Integer, ArrayList<String>> dayWiseDistances = new HashMap<>();

    private class DistanceInfo {
        String distance;
        boolean isCalculating;

        DistanceInfo(String distance, boolean isCalculating) {
            this.distance = distance;
            this.isCalculating = isCalculating;
        }
    }



    private void expandFabMenu() {
        isFabExpanded = true;
        expandedMenu.setVisibility(View.VISIBLE);

        // 메인 FAB 회전 애니메이션
        mainFab.animate()
                .rotation(45f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        // 메뉴 아이템들 페이드인 및 슬라이드 애니메이션
        expandedMenu.setAlpha(0f);
        expandedMenu.setTranslationY(50f);
        expandedMenu.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void collapseFabMenu() {
        isFabExpanded = false;

        // 메인 FAB 회전 애니메이션
        mainFab.animate()
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        // 메뉴 아이템들 페이드아웃 및 슬라이드 애니메이션
        expandedMenu.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> expandedMenu.setVisibility(View.GONE))
                .start();
    }

    private void setupFabClickListeners() {
        mainFab.setOnClickListener(v -> {
            if (isFabExpanded) {
                collapseFabMenu();
            } else {
                expandFabMenu();
            }
        });

        askAiFab.setOnClickListener(v -> {
            // AI 질문하기 처리
            collapseFabMenu();
        });

        otherPathsFab.setOnClickListener(v -> {
            // 다른 사람 일정 보기 처리
            collapseFabMenu();
        });

        // 배경 클릭시 메뉴 닫기
        findViewById(android.R.id.content).setOnClickListener(v -> {
            if (isFabExpanded) {
                collapseFabMenu();
            }
        });
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkAndShowSavingProgress() {
        // 모든 날짜에 경로가 있는지 먼저 확인
        boolean allDaysPlanned = true;
        StringBuilder missingDays = new StringBuilder();

        for (int i = 1; i <= totalDays; i++) {
            List<String> placesForDay = dayWiseDestinations.get(i);
            List<String> cachedPlaces = dataCache.getDestinations(i);

            if ((placesForDay == null || placesForDay.isEmpty()) &&
                    (cachedPlaces == null || cachedPlaces.isEmpty())) {
                allDaysPlanned = false;
                if (missingDays.length() > 0) {
                    missingDays.append(", ");
                }
                missingDays.append(i).append("일차");
            }
        }

        if (!allDaysPlanned) {
            // 빈 날짜가 있을 경우 알림 다이얼로그 표시
            new AlertDialog.Builder(this)
                    .setTitle("일정 미입력")
                    .setMessage(missingDays + "의 일정이 비어있습니다.\n모든 날짜의 일정을 입력해주세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }

        // 지도 카드뷰 캡처 수행
        CardView mapContainer = findViewById(R.id.map_container);
        captureMapView(mapContainer, success -> {
            if (success) {
                // 캡처 성공 시 저장 진행 다이얼로그 표시
                runOnUiThread(() -> {
                    if (savingDialog == null) {
                        savingDialog = new Dialog(this);
                        savingDialog.setContentView(R.layout.dialog_saving_progress);
                        savingDialog.setCancelable(false);

                        // 다이얼로그 창 크기 및 스타일 설정
                        Window window = savingDialog.getWindow();
                        if (window != null) {
                            window.setLayout(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            window.setGravity(Gravity.CENTER);

                            // 배경을 투명하게 설정
                            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            window.setDimAmount(0.6f);
                            window.setWindowAnimations(R.style.DialogAnimation);
                        }
                    }

                    // 프로그레스바와 텍스트 참조
                    ProgressBar progressBar = savingDialog.findViewById(R.id.progress_bar);
                    TextView statusText = savingDialog.findViewById(R.id.status_text);
                    TextView subStatusText = savingDialog.findViewById(R.id.sub_status_text);
                    subStatusText.setGravity(Gravity.CENTER);

                    // ProgressBar 색상 설정
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        progressBar.setIndeterminateTintList(
                                ColorStateList.valueOf(
                                        getResources().getColor(R.color.mainColor, getTheme())
                                )
                        );
                    } else {
                        progressBar.getIndeterminateDrawable().setColorFilter(
                                getResources().getColor(R.color.mainColor),
                                PorterDuff.Mode.SRC_IN
                        );
                    }

                    // 툴팁 배열 가져오기
                    tooltips = getResources().getStringArray(R.array.tooltips);

                    // 툴팁 변경 작업 시작
                    tooltipHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                            fadeIn.setDuration(1000);
                            fadeIn.setFillAfter(true);

                            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                            fadeOut.setDuration(1000);
                            fadeOut.setStartOffset(3000);
                            fadeOut.setFillAfter(true);

                            subStatusText.setText(tooltips[tooltipIndex]);
                            tooltipIndex = (tooltipIndex + 1) % tooltips.length;
                            subStatusText.startAnimation(fadeIn);
                            subStatusText.startAnimation(fadeOut);

                            tooltipHandler.postDelayed(this, 4000);
                        }
                    }, 0);

                    savingDialog.show();
                    sendRouteDataToServer();
                });
            } else {
//                // 캡처 실패 시 에러 메시지 표시
//                runOnUiThread(() ->
//                        Toast.makeText(this, "지도 캡처에 실패했습니다.", Toast.LENGTH_SHORT).show()
//                );
            }
        });
    }

    private void captureMapView(CardView mapContainer, OnCaptureCompleteListener listener) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                MapFragment mapFragment = (MapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.Map);

                if (mapFragment == null) {
                    listener.onComplete(false);
                    return;
                }

                View mapView = mapFragment.getView();
                if (mapView == null) {
                    listener.onComplete(false);
                    return;
                }

                if (naverMap == null) {
                    listener.onComplete(false);
                    return;
                }

                // 줌 컨트롤 임시 비활성화
                UiSettings uiSettings = naverMap.getUiSettings();
                boolean wasZoomEnabled = uiSettings.isZoomControlEnabled();
                uiSettings.setZoomControlEnabled(false);

                // 지도 스냅샷 생성
                naverMap.takeSnapshot(bitmap -> {
                    uiSettings.setZoomControlEnabled(wasZoomEnabled);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        try {
                            if (bitmap == null) {
                                new Handler(Looper.getMainLooper())
                                        .post(() -> listener.onComplete(false));
                                return;
                            }

                            int originalWidth = bitmap.getWidth();
                            int originalHeight = bitmap.getHeight();
                            int newWidth = (int) (originalWidth * 0.5f);
                            int newHeight = (int) (originalHeight * 0.5f);
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

                            bitmap.recycle();

                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                                    Locale.getDefault()).format(new Date());
                            String fileName = "map_" + timestamp + ".jpg";

                            File path = new File(getExternalFilesDir(null), "maps");
                            if (!path.exists()) {
                                path.mkdirs();
                            }

                            File file = new File(path, fileName);
                            FileOutputStream fos = new FileOutputStream(file);

                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos);
                            fos.close();

                            resizedBitmap.recycle();
                            new Handler(Looper.getMainLooper()).post(() -> listener.onComplete(true));

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onComplete(false));
                        } finally {
                            executor.shutdown();
                        }
                    });
                });
            } catch (Exception e) {
                listener.onComplete(false);
            }
        }, 500);
    }

    interface OnCaptureCompleteListener {
        void onComplete(boolean success);
    }

    private Handler tooltipHandler = new Handler();
    private int tooltipIndex = 0;
    private String[] tooltips;

    private void showSavingProgressDialog() {
        if (savingDialog == null) {
            savingDialog = new Dialog(this);
            savingDialog.setContentView(R.layout.dialog_saving_progress);
            savingDialog.setCancelable(false);

            // 다이얼로그 창 크기 및 스타일 설정
            Window window = savingDialog.getWindow();
            if (window != null) {
                window.setLayout(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                window.setGravity(Gravity.CENTER);

                // 배경을 투명하게 설정
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(0.6f);
                window.setWindowAnimations(R.style.DialogAnimation);
            }
        }

        // 프로그레스바와 텍스트 참조
        ProgressBar progressBar = savingDialog.findViewById(R.id.progress_bar);
        TextView statusText = savingDialog.findViewById(R.id.status_text);
        TextView subStatusText = savingDialog.findViewById(R.id.sub_status_text);
        subStatusText.setGravity(Gravity.CENTER); // 툴팁 중앙 정렬

        // ProgressBar 색상 설정 (앱의 메인 컬러로)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(getResources().getColor(R.color.mainColor, getTheme())));
        } else {
            progressBar.getIndeterminateDrawable().setColorFilter(
                    getResources().getColor(R.color.mainColor),
                    PorterDuff.Mode.SRC_IN
            );
        }

        // 툴팁 배열 가져오기
        tooltips = getResources().getStringArray(R.array.tooltips);

        // 툴팁 변경 작업 시작
        tooltipHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 페이드 인/아웃 애니메이션 설정
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(1000); // 1초 동안 페이드 인
                fadeIn.setFillAfter(true);

                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(1000); // 1초 동안 페이드 아웃
                fadeOut.setStartOffset(3000); // 페이드 인 후 3초 유지 후 페이드 아웃
                fadeOut.setFillAfter(true);

                // 애니메이션 설정 및 시작
                subStatusText.setText(tooltips[tooltipIndex]);
                tooltipIndex = (tooltipIndex + 1) % tooltips.length;
                subStatusText.startAnimation(fadeIn);
                subStatusText.startAnimation(fadeOut);

                // 4초 후에 다시 실행
                tooltipHandler.postDelayed(this, 4000);
            }
        }, 0);

        savingDialog.show();
        sendRouteDataToServer();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendRouteDataToServer() {
        // 1. 이미지 파일 로드 및 Base64 인코딩
        String base64Image = null;
        try {
            File path = new File(getExternalFilesDir(null), "maps");
            File[] files = path.listFiles();
            if (files != null && files.length > 0) {
                // 가장 최근 파일 찾기
                File latestFile = files[0];
                for (File file : files) {
                    if (file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }

                // 이미지 파일을 바이트 배열로 변환
                byte[] fileBytes = java.nio.file.Files.readAllBytes(latestFile.toPath());
                // Base64로 인코딩
                base64Image = android.util.Base64.encodeToString(fileBytes, android.util.Base64.DEFAULT);

//                // 이미지 크기 로깅
//                Log.d("TripPlanner3", "Image size (Base64): " + (base64Image.length() / 1024) + "KB");

                // 파일 삭제 (선택사항)
                latestFile.delete();
            }
        } catch (Exception e) {
//            Log.e("TripPlanner3", "이미지 로드 실패: " + e.getMessage());
        }

        // 2. 사용자 정보 가져오기
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userName = preferences.getString("user_name", "Unknown User");
        String userEmail = preferences.getString("user_email", "");

        // SharedPreferences 저장된 값들 로그 출력
//        Log.d("TripPlanner3", "SharedPreferences 저장 값:");
//        Log.d("TripPlanner3", " - user_name: " + userName);
//        Log.d("TripPlanner3", " - user_email: " + userEmail);
//        Log.d("TripPlanner3", " - is_logged_in: " + preferences.getBoolean("is_logged_in", false));

        if (userEmail.isEmpty()) {
//            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
//            Log.e("TripPlanner3", "사용자 이메일이 없습니다.");
//            return;
        }

        // 3. Request 객체 생성
        RouteSaveRequest request = new RouteSaveRequest();
        request.setUserEmail(userEmail);
        request.setTotalDays(totalDays);
        request.setAreas(selectedLocations);
        request.setRouteImage(base64Image); // 이미지 데이터 설정

        List<RouteSaveRequest.DayRouteDetail> dailyRoutes = new ArrayList<>();
        double totalDistance = 0;

        // 4. 각 일자별 데이터 추가
        for (int i = 1; i <= totalDays; i++) {
            RouteSaveRequest.DayRouteDetail dayRoute = new RouteSaveRequest.DayRouteDetail();
            dayRoute.setDay(i);
            dayRoute.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(selectedDates.get(i - 1)));

            // 캐시에서 해당 일자의 데이터를 먼저 확인
            List<String> cachedPlaces = dataCache.getDestinations(i);
            List<String> placeInfos;

            if (cachedPlaces != null && !cachedPlaces.isEmpty()) {
                placeInfos = cachedPlaces;
            } else {
                placeInfos = dayWiseDestinations.getOrDefault(i, new ArrayList<>());
            }

            // 캐시에서 거리 데이터 확인
            ArrayList<String> cachedDistances = dataCache.getDistances(i);
            List<String> distances;

            if (cachedDistances != null && !cachedDistances.isEmpty()) {
                distances = cachedDistances;
            } else {
                distances = dayWiseDistances.getOrDefault(i, new ArrayList<>());
            }

            List<RouteSaveRequest.PlaceDetail> places = new ArrayList<>();
            double dayTotalDistance = 0;

            // 일자별 장소 정보 처리
            for (int j = 0; j < placeInfos.size(); j++) {
                String[] parts = placeInfos.get(j).split(",");
                if (parts.length >= 4) {
                    RouteSaveRequest.PlaceDetail place = new RouteSaveRequest.PlaceDetail();
                    place.setPlaceName(parts[0]);
                    place.setAreaName(selectedLocations.get(0));
                    place.setLatitude(Double.parseDouble(parts[2]));
                    place.setLongitude(Double.parseDouble(parts[3]));
                    place.setSequence(j + 1);
                    places.add(place);

                    // 거리 계산
                    if (j < distances.size()) {
                        String distance = distances.get(j);
                        try {
                            dayTotalDistance += Double.parseDouble(distance.replace(" km", ""));
                        } catch (NumberFormatException e) {
//                            Log.e("TripPlanner3", "거리 파싱 오류: " + distance, e);
                        }
                    }

//                    Log.d("TripPlanner3", String.format("Day %d - Place %d: %s (%.6f, %.6f)",
//                            i, j + 1, parts[0], Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
                }
            }

            // 일자별 데이터 설정
            dayRoute.setPlaces(places);
            dayRoute.setDistances(distances);
            dailyRoutes.add(dayRoute);

            totalDistance += dayTotalDistance;

//            Log.d("TripPlanner3", String.format("Day %d - Total Places: %d, Total Distance: %.2f km",
//                    i, places.size(), dayTotalDistance));
        }

        // 5. 총 거리 설정 및 일자별 경로 설정
        request.setTotalDistance(totalDistance);
        request.setRouteDetails(dailyRoutes);

        // 6. 요청 데이터 로깅
//        Log.d("TripPlanner3", "=== 서버 요청 데이터 ===");
//        Log.d("TripPlanner3", "User Email: " + userEmail);
//        Log.d("TripPlanner3", "Total Days: " + totalDays);
//        Log.d("TripPlanner3", "Areas: " + selectedLocations);
//        Log.d("TripPlanner3", "Total Distance: " + totalDistance);
//        Log.d("TripPlanner3", "Route Image Included: " + (base64Image != null));
//        Log.d("TripPlanner3", "전체 JSON 데이터: " + new Gson().toJson(request));

        // 7. 서버 통신
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        retrofit2.Call<RouteSaveResponse> call = apiService.analyzeSaveRoute(request);

        call.enqueue(new retrofit2.Callback<RouteSaveResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RouteSaveResponse> call, retrofit2.Response<RouteSaveResponse> response) {
                if (savingDialog != null && savingDialog.isShowing()) {
                    savingDialog.dismiss();
                }

                if (response.isSuccessful() && response.body() != null) {
                    RouteSaveResponse routeResponse = response.body();
                    if ("success".equals(routeResponse.getStatus())) {
                        // 이미지 저장 결과 로깅
                        boolean imageSaved = routeResponse.isImageSaved();
//                        Log.d("TripPlanner3", "이미지 저장 결과: " + (imageSaved ? "성공" : "실패"));

                        dataCache.clearAll();

                        RouteSummary summary = convertToRouteSummary(routeResponse.getRouteSummary());
                        runOnUiThread(() -> showRouteSummaryDialog(summary));
                    } else {
//                        runOnUiThread(() -> Toast.makeText(TripPlanner3.this,
//                                "경로 저장 실패", Toast.LENGTH_SHORT).show());
                    }
                } else {
//                    runOnUiThread(() -> Toast.makeText(TripPlanner3.this,
//                            "서버 응답 실패", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RouteSaveResponse> call, Throwable t) {
                if (savingDialog != null && savingDialog.isShowing()) {
                    savingDialog.dismiss();
                }

//                Log.e("TripPlanner3", "서버 통신 실패: " + t.getMessage(), t);
//                runOnUiThread(() -> Toast.makeText(TripPlanner3.this,
//                        "서버 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static class RouteSummary {
        public String theme;
        public List<String> tags;
        public int totalCost;
        public double totalDistance;
        public List<DailyDetail> dailyDetails;
        public int routeScore;
    }

    private static class DailyDetail {
        public int day;
        public String date;
        public List<Place> places;
        public int dayTotalCost;
        public double distance;
        public List<String> distances;
    }

    private static class Place {
        String name;
        int estimatedCost;
    }

    private String formatMoney(int amount) {
        return String.format("%,d", amount);
    }

    // RouteSaveResponse를 RouteSummary로 변환하는 메서드
    private RouteSummary convertToRouteSummary(RouteSaveResponse.RouteSummary response) {
        RouteSummary summary = new RouteSummary();
        summary.theme = response.getTheme();
        summary.tags = response.getTags();
        summary.totalCost = (int)response.getTotalCost();  // double을 int로 변환
        summary.totalDistance = response.getTotalDistance();
        summary.routeScore = response.getRouteScore();

        summary.dailyDetails = new ArrayList<>();
        for (RouteSaveResponse.DailyDetail responseDetail : response.getDailyDetails()) {
            DailyDetail detail = new DailyDetail();
            detail.day = responseDetail.getDay();
            detail.date = responseDetail.getDate();
            detail.dayTotalCost = (int)responseDetail.getDayTotalCost();  // double을 int로 변환

            // 거리 정보 처리
            List<String> responseDistances = responseDetail.getDistances();
            detail.distances = responseDistances;
            detail.distance = responseDistances != null && !responseDistances.isEmpty()
                    ? Double.parseDouble(responseDistances.get(0).replace(" km", ""))
                    : 0.0;

            detail.places = new ArrayList<>();
            for (RouteSaveResponse.PlaceDetail responsePlace : responseDetail.getPlaces()) {
                Place place = new Place();
                place.name = responsePlace.getName();
                place.estimatedCost = (int)responsePlace.getEstimatedCost();  // double을 int로 변환
                detail.places.add(place);
            }

            summary.dailyDetails.add(detail);
        }

        return summary;
    }

    private void showRouteSummaryDialog(RouteSummary summary) {
        Dialog summaryDialog = new Dialog(this);
        summaryDialog.setContentView(R.layout.dialog_route_summary);
        summaryDialog.setCancelable(false);

        // 다이얼로그 창 크기 및 스타일 설정
        Window window = summaryDialog.getWindow();
        if (window != null) {
            // 화면 크기 가져오기
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;

            // 다이얼로그 최대 높이를 화면 높이의 80%로 제한
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = (int)(screenHeight * 0.8);

            window.setAttributes(params);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.6f);
        }

        // 테마 설정
        TextView themeText = summaryDialog.findViewById(R.id.themeText);
        themeText.setText("여행 테마: " + summary.theme);

        // 태그 컨테이너 설정
        FlexboxLayout tagContainer = summaryDialog.findViewById(R.id.tagContainer);
        tagContainer.removeAllViews();

        // 태그들을 생성하고 추가
        for (String tag : summary.tags) {
            TextView tagView = new TextView(this);
            tagView.setText("#" + tag);
            tagView.setTextSize(13);
            tagView.setTextColor(getResources().getColor(R.color.mainColor));

            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            tagView.setLayoutParams(params);

            tagView.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            tagView.setBackground(ContextCompat.getDrawable(this, R.drawable.tag_background));

            tagContainer.addView(tagView);
        }

        // 일자별 정보 설정
        LinearLayout daysContainer = summaryDialog.findViewById(R.id.daysContainer);
        daysContainer.removeAllViews();

        for (DailyDetail daily : summary.dailyDetails) {
            View dayView = LayoutInflater.from(this).inflate(R.layout.item_day_summary, daysContainer, false);

            TextView dayTitle = dayView.findViewById(R.id.dayTitle);
            TextView placeCount = dayView.findViewById(R.id.placeCount);
            TextView placesListCollapsed = dayView.findViewById(R.id.placesListCollapsed);
            TextView placesListExpanded = dayView.findViewById(R.id.placesListExpanded);
            TextView dayCost = dayView.findViewById(R.id.dayCost);
            TextView dayDistance = dayView.findViewById(R.id.dayDistance);
            View expandedContent = dayView.findViewById(R.id.expandedContent);

            // 일차 및 날짜 설정
            dayTitle.setText(daily.day + "일차 (" + daily.date + ")");

            // 방문 예정 장소 수 설정
            placeCount.setText("방문예정장소: " + daily.places.size() + "곳");

            // 장소 목록 생성
            StringBuilder places = new StringBuilder();
            for (Place place : daily.places) {
                if (places.length() > 0) places.append(" → ");
                String placeName = place.name.replaceAll("\\(.*?\\)", "").trim();
                places.append(placeName);
            }

            // 축소된 뷰에는 15자 이상이면 줄임
            String collapsedText = places.toString();
            if (collapsedText.length() > 15) {
                collapsedText = collapsedText.substring(0, 13) + "...";
            }
            placesListCollapsed.setText(collapsedText);

            // 확장된 뷰에는 전체 텍스트 표시
            placesListExpanded.setText(places.toString());

            // 비용과 거리 정보 설정
            dayCost.setText("예상 비용: " + formatMoney(daily.dayTotalCost) + "원");
            dayDistance.setText("이동 거리: " + String.format("%.1f", daily.distance) + "km");

            // 클릭 리스너 설정
            dayView.setOnClickListener(v -> {
                boolean isExpanded = expandedContent.getVisibility() == View.VISIBLE;
                expandedContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                placesListCollapsed.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            });

            daysContainer.addView(dayView);
        }

        // 총 요약 정보
        TextView totalDistanceText = summaryDialog.findViewById(R.id.totalDistanceText);
        TextView totalCostText = summaryDialog.findViewById(R.id.totalCostText);

        totalDistanceText.setText("총 이동거리: " + String.format("%.1f", summary.totalDistance) + "km");
        totalCostText.setText("총 예상비용: " + formatMoney(summary.totalCost) + "원");

        // 확인 버튼
        CardView confirmButton = summaryDialog.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> {
            saveTravelDataToCacheAndNavigate(summary); // 데이터를 캐시에 저장하고 MainActivity로 전달
            dataCache.clearAll();
            summaryDialog.dismiss();
        });

        summaryDialog.show();
    }

    // 여행 데이터 저장되면 MainActivity로 돌아가는 메서드
    private void saveTravelDataToCacheAndNavigate(RouteSummary summary) {

        // MainActivity로 데이터 전달
        Intent intent = new Intent(TripPlanner3.this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }




    // dp를 pixel로 변환하는 유틸리티 메서드
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }



}