package com.ysu.capstone;

// Android core
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Calendar view
import com.google.gson.Gson;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener;

// Custom decorator
import com.ysu.capstone.decorators.EndDateDecorator;
import com.ysu.capstone.decorators.PastDateDecorator;
import com.ysu.capstone.decorators.RangeDateDecorator;
import com.ysu.capstone.decorators.StartDateDecorator;
import com.ysu.capstone.decorators.TodayDecorator;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.AutoRouteRequest;
import com.ysu.capstone.network.AutoRouteResponse;
import com.ysu.capstone.network.RetrofitClient;

// Java util
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 여행 계획 날짜 선택 화면
public class TripPlanner2 extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView imgRectangle1;
    private TextView imgRectangle2;
    private TextView btnNext;
    private ImageView back;

    private long numOfNights = 0;
    private long numOfDays = 0;
    private List<CalendarDay> selectedDates;

    private ArrayList<String> selectedLocations;
    private ArrayList<String> selectedTags;

    // 액티비티 생성 시 초기화
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_planner2);

        // Intent로부터 데이터 받기
        Intent intent = getIntent();
        selectedLocations = intent.getStringArrayListExtra("selectedLocations");
        selectedTags = intent.getStringArrayListExtra("selectedTags");


        clearPreviousTripData();
        if (selectedTags != null && !selectedTags.isEmpty()) {
            initializeRouteStatus();
        }

        initializeViews();
        setupCalendarView();
        setupBackButton();
        setupNextButton();

//        // 수신 데이터 로깅
//        if (selectedLocations != null) {
//            Log.d("TripPlanner2", "받은 지역: " + selectedLocations.toString());
//        } else {
//            Log.e("TripPlanner2", "지역 데이터가 null입니다.");
//        }
//
//        if (selectedTags != null) {
//            Log.d("TripPlanner2", "받은 태그: " + selectedTags.toString());
//        } else {
//            Log.e("TripPlanner2", "태그 데이터가 null입니다.");
//        }
    }

    private void clearPreviousTripData() {
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 이전 경로 데이터 삭제
        editor.remove("auto_route_schedule");
        editor.remove("route_generation_status");
        editor.remove("day_wise_destinations");

        // 이전 장소 데이터 삭제
        editor.remove("place_names");
        editor.remove("place_addresses");
        editor.remove("latitudes");
        editor.remove("longitudes");

        // 채팅 히스토리 삭제
        SharedPreferences chatPrefs = getSharedPreferences("ChatHistory", MODE_PRIVATE);
        chatPrefs.edit().clear().apply();

        editor.apply();

        Log.d("TripPlanner2", "이전 여행 데이터가 초기화되었습니다.");

    }

    private void initializeRouteStatus() {
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("route_generation_status", "not_started");
        editor.apply();
        Log.d("TripPlanner2", "경로 생성 상태 초기화됨: not_started");
    }

    // UI 요소 초기화
    private void initializeViews() {
        calendarView = findViewById(R.id.calendar_view);
        imgRectangle1 = findViewById(R.id.img_rectangle1);
        imgRectangle2 = findViewById(R.id.img_rectangle2);
        btnNext = findViewById(R.id.btn_next);
        back = findViewById(R.id.ic_back);

        btnNext.setText("고민중이에요...");
    }

    // 캘린더 뷰 설정
    private void setupCalendarView() {
        Calendar minDate = Calendar.getInstance();
        calendarView.state().edit()
                .setMinimumDate(CalendarDay.from(minDate))
                .commit();

        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
        calendarView.addDecorator(new PastDateDecorator(this));
        calendarView.addDecorator(new TodayDecorator(this));

        setupCalendarRangeListener();
    }

    // 날짜 범위 선택 리스너 설정
    private void setupCalendarRangeListener() {
        calendarView.setOnRangeSelectedListener((widget, dates) -> {
            if (dates != null && dates.size() > 0) {
                handleDateRangeSelection(widget, dates);
            }
        });
    }

    // 선택된 날짜 범위 처리
    private void handleDateRangeSelection(MaterialCalendarView widget, List<CalendarDay> dates) {
        calendarView.clearSelection();
        widget.removeDecorators();

        CalendarDay startDate = dates.get(0);
        CalendarDay endDate = dates.get(dates.size() - 1);

        updateDateDisplays(startDate, endDate);
        updateDecorators(widget, startDate, endDate);
        calculateTripDuration(startDate, endDate);

        selectedDates = new ArrayList<>(dates);
        btnNext.setText(numOfNights + "박 " + numOfDays + "일 여행 예정이에요!");
    }

    // 선택된 날짜 화면에 표시
    private void updateDateDisplays(CalendarDay startDate, CalendarDay endDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일", Locale.KOREA);

        String startDateString = formatter.format(startDate.getDate());
        String endDateString = formatter.format(endDate.getDate());

        imgRectangle1.setText(startDateString);
        imgRectangle1.setTypeface(null, android.graphics.Typeface.BOLD);
        imgRectangle2.setText(endDateString);
        imgRectangle2.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    // 캘린더 데코레이터 업데이트
    private void updateDecorators(MaterialCalendarView widget, CalendarDay startDate, CalendarDay endDate) {
        widget.addDecorator(new StartDateDecorator(this, startDate));
        widget.addDecorator(new EndDateDecorator(this, endDate));
        widget.addDecorator(new RangeDateDecorator(this, startDate, endDate));
        widget.addDecorator(new TodayDecorator(this));
    }

    // 여행 기간 계산
    private void calculateTripDuration(CalendarDay startDate, CalendarDay endDate) {
        long diffInMillies = endDate.getDate().getTime() - startDate.getDate().getTime();
        numOfDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
        numOfNights = numOfDays - 1;
    }

    // 뒤로가기 버튼 설정
    private void setupBackButton() {
        back.setOnClickListener(view -> {
            Intent backIntent = new Intent(TripPlanner2.this, TripPlanner1.class);
            startActivity(backIntent);
            finish();
        });
    }

    // 다음 버튼 설정
    private void setupNextButton() {
        btnNext.setOnClickListener(view -> {
            if (numOfDays > 0) {
                navigateToNextScreen(selectedLocations, selectedTags);
            } else {
                btnNext.setText("날짜를 먼저 선택해주세요!");
            }
        });
    }

    // 다음 화면으로 이동
    private void navigateToNextScreen(ArrayList<String> selectedLocations, ArrayList<String> selectedTags) {
        // 먼저 TripPlanner3로 이동
        Intent intent = new Intent(TripPlanner2.this, TripPlanner3.class);
        intent.putExtra("numOfNights", (int) numOfNights);
        intent.putExtra("numOfDays", (int) numOfDays);
        intent.putParcelableArrayListExtra("selectedDates", new ArrayList<>(selectedDates));
        intent.putStringArrayListExtra("selectedLocations", selectedLocations);
        intent.putStringArrayListExtra("selectedTags", selectedTags);
        startActivity(intent);

        // 태그가 선택된 경우에만 자동 경로 생성 요청
        if (selectedTags != null && !selectedTags.isEmpty()) {
            // 경로 생성 상태를 pending으로 설정
            SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("route_generation_status", "pending");
            editor.apply();
            Log.d("TripPlanner2", "경로 생성 시작: pending");

            // 백그라운드에서 경로 생성 요청
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            String startDate = formatter.format(selectedDates.get(0).getDate());
            String endDate = formatter.format(selectedDates.get(selectedDates.size() - 1).getDate());

            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            AutoRouteRequest request = new AutoRouteRequest(
                    selectedLocations,
                    selectedTags,
                    (int) numOfDays,
                    (int) numOfNights,
                    startDate,
                    endDate
            );

            Log.d("TripPlanner2", "전송할 JSON 데이터: " + new Gson().toJson(request));

            Call<AutoRouteResponse> call = apiService.getAutoRoute(request);
            call.enqueue(new Callback<AutoRouteResponse>() {
                @Override
                public void onResponse(Call<AutoRouteResponse> call, Response<AutoRouteResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AutoRouteResponse routeResponse = response.body();

                        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        // 경로 데이터가 있는 경우에만 처리
                        if (routeResponse.getSchedule() != null) {
                            try {
                                // schedule JSON 저장
                                String scheduleJson = new Gson().toJson(routeResponse.getSchedule());
                                editor.putString("auto_route_schedule", scheduleJson);

                                Log.d("TripPlanner2", "저장된 schedule JSON: " + scheduleJson);

                                // 장소 정보 리스트 초기화
                                ArrayList<String> placeNames = new ArrayList<>();
                                ArrayList<String> placeAddresses = new ArrayList<>();
                                ArrayList<String> latitudes = new ArrayList<>();
                                ArrayList<String> longitudes = new ArrayList<>();

                                // Map에서 직접 장소 정보 추출
                                for (Map.Entry<String, List<AutoRouteResponse.Place>> entry : routeResponse.getSchedule().entrySet()) {
                                    Log.d("TripPlanner2", String.format(
                                            "일차 %s의 장소 데이터 처리 시작", entry.getKey()
                                    ));

                                    for (AutoRouteResponse.Place place : entry.getValue()) {
                                        placeNames.add(place.getName());
                                        placeAddresses.add(place.getAddress());
                                        longitudes.add(place.getLongitude());  // 경도 먼저
                                        latitudes.add(place.getLatitude());    // 위도 나중에

                                        Log.d("TripPlanner2", String.format(
                                                "장소 정보 추가: %s (주소: %s, 위도: %s, 경도: %s)",
                                                place.getName(), place.getAddress(),
                                                place.getLatitude(), place.getLongitude()
                                        ));
                                    }
                                }

                                // 상태를 completed로 설정
                                editor.putString("route_generation_status", "completed");

                                // 추출한 장소 정보 저장
                                editor.putString("place_names", new Gson().toJson(placeNames));
                                editor.putString("place_addresses", new Gson().toJson(placeAddresses));
                                editor.putString("latitudes", new Gson().toJson(latitudes));
                                editor.putString("longitudes", new Gson().toJson(longitudes));

                                // 변경사항 즉시 적용
                                editor.apply();

                                // 저장된 데이터 확인
                                Log.d("TripPlanner2", "=== 저장된 데이터 확인 ===");
                                Log.d("TripPlanner2", "상태: " + prefs.getString("route_generation_status", ""));
                                Log.d("TripPlanner2", "장소명 목록: " + prefs.getString("place_names", ""));
                                Log.d("TripPlanner2", "주소 목록: " + prefs.getString("place_addresses", ""));
                                Log.d("TripPlanner2", "위도 목록: " + prefs.getString("latitudes", ""));
                                Log.d("TripPlanner2", "경도 목록: " + prefs.getString("longitudes", ""));

                                Log.d("TripPlanner2", "경로 생성 완료. 상태: completed");
                                Log.d("TripPlanner2", "저장된 장소 개수: " + placeNames.size());

                            } catch (Exception e) {
                                Log.e("TripPlanner2", "데이터 처리 오류: " + e.getMessage());
                                editor.putString("route_generation_status", "error");
                                editor.apply();
                            }
                        } else {
                            Log.e("TripPlanner2", "경로 데이터 없음");
                            editor.putString("route_generation_status", "error");
                            editor.apply();
                        }

                        Log.d("TripPlanner2", "응답 데이터: " + new Gson().toJson(routeResponse));
                    } else {
                        // 실패 시 상태 업데이트
                        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                        prefs.edit().putString("route_generation_status", "error").apply();

                        Log.e("TripPlanner2", "서버 응답 실패: 코드=" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<AutoRouteResponse> call, Throwable t) {
                    // 네트워크 오류 시 상태 업데이트
                    SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                    prefs.edit().putString("route_generation_status", "error").apply();

                    Log.e("TripPlanner2", "데이터 전송 실패: " + t.getMessage(), t);
                }
            });
        }
    }

}