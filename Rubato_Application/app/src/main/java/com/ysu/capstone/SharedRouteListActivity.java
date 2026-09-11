package com.ysu.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RetrofitClient;
import com.ysu.capstone.network.SharedRouteRequest;
import com.ysu.capstone.network.SharedRouteResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class SharedRouteListActivity extends AppCompatActivity {

    private LinearLayout routeListContainer;
    private TextView tripNameTextView;
    private List<SharedRouteResponse.Route> allRoutes;
    private Spinner periodSpinner;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_route_list);

        // 뒤로가기 버튼 초기화
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // ProgressBar 초기화
        loadingProgressBar = findViewById(R.id.loading_progress);

        // UI 초기화
        routeListContainer = findViewById(R.id.route_list_container);
        tripNameTextView = findViewById(R.id.trip_name);
        periodSpinner = findViewById(R.id.spinner_date);

        // 스피너 설정
        setupPeriodSpinner();

        // 전달받은 데이터 수신
        ArrayList<String> receivedLocations = getIntent().getStringArrayListExtra("selectedLocations");
        String receivedTravelPeriod = getIntent().getStringExtra("travelPeriod");

        // 지역명 표시
        if (receivedLocations != null && !receivedLocations.isEmpty()) {
            tripNameTextView.setText(String.join(", ", receivedLocations));
        } else {
            tripNameTextView.setText("선택된 지역 없음");
        }

        // 스피너 초기값 설정
        if (receivedTravelPeriod != null && !receivedTravelPeriod.isEmpty()) {
            int position = getPeriodPosition(receivedTravelPeriod);
            periodSpinner.setSelection(position);
        }

        // 경로 데이터 가져오기
        fetchRoutes(receivedLocations);
    }

    private void fetchRoutes(List<String> regions) {
        showLoading(); // 로딩 시작

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        SharedRouteRequest request = new SharedRouteRequest(regions);

        apiService.getSharedRoutes(request).enqueue(new Callback<SharedRouteResponse>() {
            @Override
            public void onResponse(Call<SharedRouteResponse> call, Response<SharedRouteResponse> response) {
                hideLoading(); // 로딩 종료

                if (response.isSuccessful() && response.body() != null) {
                    SharedRouteResponse sharedRouteResponse = response.body();
                    allRoutes = sharedRouteResponse.getData(); // 모든 루트 저장

                    if (allRoutes == null || allRoutes.isEmpty()) {
                        handleNoRoutes(sharedRouteResponse.getTotalCount());
                    } else {
                        List<SharedRouteResponse.Route> filteredRoutes = filterRoutesByPeriod(allRoutes);
                        if (filteredRoutes.isEmpty()) {
                            handleNoRoutes(sharedRouteResponse.getTotalCount());
                        } else {
                            displayRoutes(filteredRoutes);
                        }
                    }
                } else {
                    Toast.makeText(SharedRouteListActivity.this, "서버 응답 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SharedRouteResponse> call, Throwable t) {
                hideLoading(); // 로딩 종료
                t.printStackTrace();
                Toast.makeText(SharedRouteListActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRoutes(List<SharedRouteResponse.Route> routes) {
        routeListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (SharedRouteResponse.Route route : routes) {
            Log.d("SharedRouteActivity", "Route ID: " + route.getRouteId());
            Log.d("SharedRouteActivity", "Route Image: " + route.getRouteImage());

            View routeView = inflater.inflate(R.layout.inc_route_ui, routeListContainer, false);

            TextView userNameTextView = routeView.findViewById(R.id.user_name_text);
            TextView routeCountTextView = routeView.findViewById(R.id.route_count_text);
            TextView routeDistanceTextView = routeView.findViewById(R.id.route_distance_text);
            TextView routeExpenseTextView = routeView.findViewById(R.id.route_expense_text);
            ImageView routeImageView = routeView.findViewById(R.id.route_image);
            ImageView copyButton = routeView.findViewById(R.id.copy_button);

            userNameTextView.setText("작성자: " + route.getUserName());
            routeCountTextView.setText("방문 여행지: " + route.getRouteCount() + "개");
            routeDistanceTextView.setText("이동거리: " + route.getRouteDistance() + "km");
            routeExpenseTextView.setText("예상경비: " + route.getRouteExpense() + "원");

            // 이미지 처리 추가
            String base64Image = route.getRouteImage();
            if (base64Image != null && !base64Image.isEmpty()) {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                routeImageView.setImageBitmap(bitmap);
            } else {
                routeImageView.setImageResource(R.drawable.img_image_notfound);
            }

            copyButton.setOnClickListener(v -> showCopyConfirmDialog(route));
            routeListContainer.addView(routeView);
        }
    }

    private void showCopyConfirmDialog(SharedRouteResponse.Route route) {
        new AlertDialog.Builder(this)
                .setTitle("경로 복사")
                .setMessage("정말 이 경로를 복사하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    // route_detail을 날짜별로 정리
                    Map<String, List<RouteDetail>> dateGroupedDetails = new HashMap<>();
                    if (route.getRouteDetails() != null) {
                        for (SharedRouteResponse.RouteDetail detail : route.getRouteDetails()) {
                            dateGroupedDetails
                                    .computeIfAbsent(detail.getDate(), k -> new ArrayList<>())
                                    .add(new RouteDetail(
                                            detail.getPlaceName(),
                                            "주소",
                                            String.valueOf(detail.getPlaceLatitude()),
                                            String.valueOf(detail.getPlaceLongitude()),
                                            detail.getRouteSeq()
                                    ));
                        }
                    }

                    // 날짜 정보 처리
                    List<String> sortedDates = new ArrayList<>(dateGroupedDetails.keySet());
                    Collections.sort(sortedDates);

                    // CalendarDay 리스트 생성
                    ArrayList<CalendarDay> selectedCalendarDays = new ArrayList<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
                    for (String dateStr : sortedDates) {
                        try {
                            Date date = dateFormat.parse(dateStr);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            CalendarDay calendarDay = CalendarDay.from(
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH) + 1,
                                    cal.get(Calendar.DAY_OF_MONTH)
                            );
                            selectedCalendarDays.add(calendarDay);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    // TripPlanner3로 데이터 전달
                    Intent intent = new Intent(this, TripPlanner3.class);

                    // 기존 선택된 정보 전달
                    intent.putStringArrayListExtra("selectedLocations",
                            getIntent().getStringArrayListExtra("selectedLocations"));

                    // 날짜 정보 전달
                    intent.putParcelableArrayListExtra("selectedDates", selectedCalendarDays);

                    // 여행 일수 계산
                    int numOfDays = dateGroupedDetails.size();
                    int numOfNights = numOfDays - 1;
                    intent.putExtra("numOfDays", numOfDays);
                    intent.putExtra("numOfNights", numOfNights);

                    // currentDay 설정
                    intent.putExtra("currentDay", 1);

                    // 각 날짜별 데이터를 배열에 저장
                    for (int day = 1; day <= sortedDates.size(); day++) {
                        String currentDate = sortedDates.get(day - 1);
                        List<RouteDetail> dayDetails = dateGroupedDetails.get(currentDate);

                        ArrayList<String> selectedPlaces = new ArrayList<>();
                        ArrayList<String> selectedPlaceAddresses = new ArrayList<>();
                        ArrayList<String> placeLatitudes = new ArrayList<>();
                        ArrayList<String> placeLongitudes = new ArrayList<>();

                        // 순서대로 정렬
                        Collections.sort(dayDetails, (a, b) -> a.sequence - b.sequence);

                        // 해당 날짜의 모든 장소 정보 추가
                        for (RouteDetail detail : dayDetails) {
                            selectedPlaces.add(detail.placeName);
                            selectedPlaceAddresses.add(detail.address);
                            placeLatitudes.add(detail.latitude);
                            placeLongitudes.add(detail.longitude);
                        }

                        // 해당 날짜의 데이터를 Intent에 추가
                        if (day == 1) {
                            // 첫째 날은 기본 키로도 저장 (TripPlanner3의 기존 로직 호환성을 위해)
                            intent.putStringArrayListExtra("selectedPlaces", selectedPlaces);
                            intent.putStringArrayListExtra("selectedPlaceAddresses", selectedPlaceAddresses);
                            intent.putStringArrayListExtra("placeLatitudes", placeLatitudes);
                            intent.putStringArrayListExtra("placeLongitudes", placeLongitudes);
                        }

                        // 모든 날짜의 데이터를 날짜별 키로 저장
                        intent.putStringArrayListExtra("placeNames_" + day, selectedPlaces);
                        intent.putStringArrayListExtra("placeAddresses_" + day, selectedPlaceAddresses);
                        intent.putStringArrayListExtra("latitudes_" + day, placeLatitudes);
                        intent.putStringArrayListExtra("longitudes_" + day, placeLongitudes);
                    }

                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("아니오", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }


    private void handleNoRoutes(int totalCount) {
        routeListContainer.removeAllViews();

        TextView noDataMessage = new TextView(this);
        noDataMessage.setText("아직 일치하는 데이터가 없어요 ㅠ.ㅠ");
        noDataMessage.setTextSize(18);
        noDataMessage.setGravity(Gravity.CENTER);
        noDataMessage.setPadding(16, 16, 16, 8);
        routeListContainer.addView(noDataMessage);

        TextView totalCountMessage = new TextView(this);
        totalCountMessage.setText("전체 데이터: " + totalCount + "개");
        totalCountMessage.setTextSize(16);
        totalCountMessage.setGravity(Gravity.CENTER);
        totalCountMessage.setPadding(16, 8, 16, 16);
        routeListContainer.addView(totalCountMessage);
    }

    private List<SharedRouteResponse.Route> filterRoutesByPeriod(List<SharedRouteResponse.Route> routes) {
        String selectedPeriod = periodSpinner.getSelectedItem().toString();
        int requiredNights = Integer.parseInt(selectedPeriod.split("박")[0]);

        List<SharedRouteResponse.Route> filteredRoutes = new ArrayList<>();

        for (SharedRouteResponse.Route route : routes) {
            if (route.getRouteDetails() == null || route.getRouteDetails().isEmpty()) {
                continue;
            }

            String minDate = null;
            String maxDate = null;
            for (SharedRouteResponse.RouteDetail detail : route.getRouteDetails()) {
                String currentDate = detail.getDate();
                if (minDate == null || currentDate.compareTo(minDate) < 0) {
                    minDate = currentDate;
                }
                if (maxDate == null || currentDate.compareTo(maxDate) > 0) {
                    maxDate = currentDate;
                }
            }

            if (minDate != null && maxDate != null) {
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(minDate);
                    java.time.LocalDate endDate = java.time.LocalDate.parse(maxDate);
                    long nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

                    if (nights == requiredNights) {
                        filteredRoutes.add(route);
                    }
                } catch (Exception e) {
                    Log.e("DateParsing", "날짜 파싱 오류", e);
                }
            }
        }

        return filteredRoutes;
    }


    private void setupPeriodSpinner() {
        String[] periods = new String[14];
        periods[0] = "1박2일";
        for (int i = 2; i <= 14; i++) {
            periods[i-1] = (i) + "박" + (i+1) + "일";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPeriod = periods[position];
                if (allRoutes != null) {
                    showLoading(); // 로딩 시작
                    List<SharedRouteResponse.Route> filteredRoutes = filterRoutesByPeriod(allRoutes);
                    displayRoutes(filteredRoutes);
                    hideLoading(); // 로딩 종료
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private int getPeriodPosition(String period) {
        if (period.equals("1박2일")) return 0;
        String nights = period.split("박")[0];
        try {
            int nightCount = Integer.parseInt(nights);
            return nightCount - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static class RouteDetail {
        String placeName;
        String address;
        String latitude;
        String longitude;
        int sequence;

        RouteDetail(String placeName, String address, String latitude, String longitude, int sequence) {
            this.placeName = placeName;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.sequence = sequence;
        }
    }

    private void showLoading() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }
}
