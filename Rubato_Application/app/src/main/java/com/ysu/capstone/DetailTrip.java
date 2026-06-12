package com.ysu.capstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// 추가할 올바른 import
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.overlay.Marker;
import com.google.gson.Gson;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RetrofitClient;
import com.ysu.capstone.network.RouteDetailRequest;
import com.ysu.capstone.network.RouteDetailResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.overlay.OverlayImage;
import androidx.cardview.widget.CardView;
import okhttp3.OkHttpClient;


public class DetailTrip extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView back;
    private List<RouteDetailResponse.RouteDetail> routeDetails = new ArrayList<>();
    private Map<String, List<RouteDetailResponse.RouteDetail>> groupedDetails = new LinkedHashMap<>();
    private List<String> areaNames = new ArrayList<>(); // 지역명 저장 변수
    private int totalDays = 0; // 여행 총 일수
    private int currentDay = 1; // 현재 선택된 날짜
    private List<String> sortedDates = new ArrayList<>(); // 정렬된 날짜 목록
    private NaverMap naverMap;
    private List<Marker> currentMarkers = new ArrayList<>();
    private List<PolylineOverlay> polylines = new ArrayList<>();
    private Map<Marker, Boolean> markerExpandStates = new HashMap<>();
    private Map<Marker, String> markerFullNames = new HashMap<>();
    private String apiKeyId;
    private String apiKey;
    private final OkHttpClient client = new OkHttpClient();



    private final int[] markerColors = {
            0xFFB5E6B5,  // Pastel Green
            0xFFB5D8FF,  // Pastel Blue
            0xFFFFB5D8,  // Pastel Pink
            0xFFE1B5FF,  // Pastel Purple
            0xFFFFD6B5,  // Pastel Orange
            0xFFB5F4FF   // Pastel Cyan
    };
    // UI 컴포넌트
    private TextView travelDateView, placeTextView, dayNumView, dateView, dayTextView;
    private LinearLayout listLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_plan);


        // Intent로 전달된 day_time 값 읽기
        Intent intent = getIntent();
        String selectedDayTime = intent.getStringExtra("selected_day_time");

        if (selectedDayTime != null) {
            currentDay = parseDayFromTime(selectedDayTime); // day_time에서 날짜 숫자를 추출
            displayDayDetails(currentDay); // 초기 상태로 선택된 날짜의 세부정보 표시
        } else {
            Toast.makeText(this, "날짜 정보가 전달되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }



        apiKeyId = getResources().getString(R.string.naver_api_key_id);
        apiKey = getResources().getString(R.string.naver_api_key);
        // UI 요소 초기화
        travelDateView = findViewById(R.id.travel_date);
        placeTextView = findViewById(R.id.place_d);
        dayNumView = findViewById(R.id.Day_num);
        dateView = findViewById(R.id.date);
        dayTextView = findViewById(R.id.day);
        listLayout = findViewById(R.id.list_layout);
        back = findViewById(R.id.back);

        // 뒤로가기 버튼 리스너
        back.setOnClickListener(view -> {
            Intent backIntent = new Intent(DetailTrip.this, MainActivity.class);
            startActivity(backIntent);
        });
        // 캐시에서 이메일 가져오기
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userEmail = userPrefs.getString("user_email", "");

        if (!userEmail.isEmpty()) {
            fetchRouteDetails(userEmail);
        } else {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
        }


    }

    private void fetchRouteDetails(String email) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        RouteDetailRequest request = new RouteDetailRequest(email);

        Call<RouteDetailResponse> call = apiService.getRouteDetails(request);
        call.enqueue(new Callback<RouteDetailResponse>() {
            @Override
            public void onResponse(Call<RouteDetailResponse> call, Response<RouteDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RouteDetailResponse detailResponse = response.body();

                    if (detailResponse.isSuccess()) {
                        routeDetails = detailResponse.getDetails();
                        areaNames = detailResponse.getAreaNames();

                        Log.d("DetailTrip", "Fetched route details successfully.");
                        Log.d("DetailTrip", "Area Names: " + areaNames);

                        for (RouteDetailResponse.RouteDetail detail : routeDetails) {
                            Log.d("RouteDetail", "ID: " + detail.getRouteDetailId() +
                                    ", Name: " + detail.getPlaceName() +
                                    ", Latitude: " + detail.getPlaceLatitude() +
                                    ", Longitude: " + detail.getPlaceLongitude() +
                                    ", Date: " + detail.getDate() +
                                    ", Sequence: " + detail.getRouteSeq());
                        }

                        calculateTotalDays();
                        updateUI();
                    } else {
                        Toast.makeText(DetailTrip.this, "일정 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DetailTrip.this, "서버 응답 오류입니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RouteDetailResponse> call, Throwable t) {
                Toast.makeText(DetailTrip.this, "서버 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        initNaverMap();

    }
        //날짜데이터 추출 ㅗ
    private int parseDayFromTime(String dayTime) {
        try {
            // 숫자만 추출 (예: "Day 2" -> 2)
            String dayNumber = dayTime.replaceAll("[^0-9]", "");
            return Integer.parseInt(dayNumber);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 1; // 기본값: 1일차
        }
    }


    // 네이버 지도 초기화
    private void initNaverMap() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.Map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.Map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }


    private void calculateTotalDays() {
        for (RouteDetailResponse.RouteDetail detail : routeDetails) {
            groupedDetails.computeIfAbsent(detail.getDate(), k -> new ArrayList<>()).add(detail);
        }

        // 날짜 정렬
        sortedDates.addAll(groupedDetails.keySet());
        sortedDates.sort(Comparator.comparing(this::parseDate));

        totalDays = sortedDates.size(); // 날짜 개수로 총 일수 계산

        Log.d("DetailTrip", "Total Days: " + totalDays);
        Log.d("DetailTrip", "Sorted Dates: " + sortedDates);
    }

    private Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }

    private void updateUI() {
        String travelDateText = (totalDays - 1) + "박 " + totalDays + "일의 여행";
        travelDateView.setText(travelDateText);

        if (!areaNames.isEmpty()) {
            placeTextView.setText(String.join(", ", areaNames));
        }

        // 초기 날짜 데이터를 화면에 표시
        displayDayDetails(currentDay);
    }

    //거리 계산하는 메서드
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }



    private void displayDayDetails(int day) {
        if (day > totalDays || day < 1) return;

        String currentDate = sortedDates.get(day - 1);
        List<RouteDetailResponse.RouteDetail> detailsForDay = groupedDetails.get(currentDate);

        // Day 번호 및 날짜 텍스트 업데이트
        dayNumView.setText(String.valueOf(day));
        dateView.setText(formatDate(currentDate));

        // 날짜 이름 텍스트 업데이트
        if (currentDay == 1) {
            dayTextView.setText("첫날");
        } else if (currentDay == totalDays) {
            dayTextView.setText("마지막날");
        } else {
            String koreanDay = DateHelper.getKoreanDayString(currentDay);
            dayTextView.setText(koreanDay);
        }

        // 이전/다음일정보기 버튼 업데이트
        CardView prevButton = findViewById(R.id.prevScheduleButton);
        CardView nextButton = findViewById(R.id.nextScheduleButton);

        if (currentDay == 1) {
            prevButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
        } else if (currentDay == totalDays) {
            prevButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.GONE);
        } else {
            prevButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }

        prevButton.setOnClickListener(v -> {
            navigateToDay(-1); // 이전날 이동
        });

        nextButton.setOnClickListener(v -> {
            navigateToDay(1); // 다음날 이동
        });

        // 일정 목록 갱신
        listLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (detailsForDay != null) {
            for (int i = 0; i < detailsForDay.size(); i++) {
                RouteDetailResponse.RouteDetail detail = detailsForDay.get(i);
                String placeName = detail.getPlaceName();

                // 장소 표시 레이아웃
                View placeView = inflater.inflate(R.layout.inc_detail_plan, listLayout, false);
                TextView placeNumTextView = placeView.findViewById(R.id.travel_num_text);
                placeNumTextView.setText(String.valueOf(i + 1));
                TextView placeNameTextView = placeView.findViewById(R.id.travel_info_text);
                placeNameTextView.setText(placeName);
//
//                // X 버튼
//                ImageView deleteButton = placeView.findViewById(R.id.x_ic_button);
//                int indexToRemove = i;
//                deleteButton.setOnClickListener(v -> {
//                    detailsForDay.remove(indexToRemove);
//                    displayDayDetails(day); // UI 갱신 및 거리 재계산
//                });

                // 리스트에 추가
                listLayout.addView(placeView);

                // 마지막 장소가 아닐 경우 거리 표시
                if (i < detailsForDay.size() - 1) {
                    RouteDetailResponse.RouteDetail nextDetail = detailsForDay.get(i + 1);

                    // 거리 계산
                    double distance = calculateDistance(
                            detail.getPlaceLatitude(),
                            detail.getPlaceLongitude(),
                            nextDetail.getPlaceLatitude(),
                            nextDetail.getPlaceLongitude()
                    );

                    // 거리 표시 레이아웃 추가
                    View distanceView = inflater.inflate(R.layout.inc_plan_order, listLayout, false);
                    TextView distanceTextView = distanceView.findViewById(R.id.order_distance);
                    distanceTextView.setText(String.format(Locale.KOREA, "%.2f km", distance));
                    listLayout.addView(distanceView);
                }
            }
        }

        // 지도 마커 업데이트
        if (naverMap != null && detailsForDay != null) {
            List<LatLng> locations = new ArrayList<>();
            List<String> placeNames = new ArrayList<>();

            for (RouteDetailResponse.RouteDetail detail : detailsForDay) {
                locations.add(new LatLng(detail.getPlaceLatitude(), detail.getPlaceLongitude()));
                placeNames.add(detail.getPlaceName());
            }

            updateMapWithLocation(locations, placeNames, false);
        }
    }


    private void updateMapWithLocation(List<LatLng> locations, List<String> placeNames, boolean isDefaultLocation) {
        if (naverMap == null) {
            Log.e("DetailTrip", "naverMap이 null입니다");
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
        }
    }


    private Bitmap createCustomMarkerBitmap(String placeName, int number, boolean isExpanded) {
        View markerView = LayoutInflater.from(this).inflate(R.layout.custom_marker_layout, null);

        TextView numberView = markerView.findViewById(R.id.marker_number);
        TextView placeNameView = markerView.findViewById(R.id.marker_place_name);
        CardView cardView = markerView.findViewById(R.id.marker_card);

        // 마커 색상 설정
        int markerColor = markerColors[(number - 1) % markerColors.length];
        cardView.setCardBackgroundColor(markerColor);

        // 숫자는 검정색으로
        numberView.setTextColor(0xFF000000);
        numberView.setText(String.valueOf(number));

        // 장소 이름 처리
        if (!isExpanded && placeName.length() > 8) {
            placeNameView.setText(placeName.substring(0, 8) + "...");
        } else {
            placeNameView.setText(placeName);
        }

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



    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(true);

        // 데이터가 이미 로드되어 있다면 지도 업데이트
        if (!sortedDates.isEmpty()) {
            displayDayDetails(currentDay);
        }
    }

    // 네이버 지도 관련 메서드_5 :: 색상이 있는 경로를 요청하는 메서드
    private void fetchRoutePathWithColor(LatLng start, LatLng end, int segmentIndex) {
        String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving" +
                "?start=" + start.longitude + "," + start.latitude +
                "&goal=" + end.longitude + "," + end.latitude +
                "&option=traoptimal";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
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
                    }
                }
            }
        });
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

    private void navigateToDay(int direction) {
        int newDay = currentDay + direction;

        if (newDay >= 1 && newDay <= totalDays) {
            currentDay = newDay;
            displayDayDetails(currentDay);
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MM.dd", Locale.KOREA);
            Date date = originalFormat.parse(dateString);
            return displayFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return dateString;
        }
    }

}