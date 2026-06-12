package com.ysu.capstone;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.RetrofitClient;
import com.ysu.capstone.network.ThemeRouteRequest;
import com.ysu.capstone.network.ThemeRouteResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ThemeRouteActivity extends AppCompatActivity {

    private TextView titleTextView;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_route);

        // 뒤로가기 버튼 초기화
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // ProgressBar 초기화
        loadingProgressBar = findViewById(R.id.loading_progress);

        // 전달받은 데이터 가져오기
        String buttonName = getIntent().getStringExtra("buttonName");
        ArrayList<String> themes = getIntent().getStringArrayListExtra("themes");

        // 제목과 테마값을 로그로 출력
        Log.d("ThemeRouteActivity", "Received buttonName: " + buttonName);
        Log.d("ThemeRouteActivity", "Received themes: " + (themes != null ? themes.toString() : "null"));


        // 타이틀 설정
        titleTextView = findViewById(R.id.theme_name);  // 레이아웃의 실제 TextView ID로 변경 필요
        if (titleTextView != null && buttonName != null) {
            titleTextView.setText(buttonName);
        }

        // 데이터를 사용하여 테마 루트 요청
        if (themes != null) {
            getThemeRoutes(themes);
        }
    }



    private void getThemeRoutes(List<String> themes) {
        showLoading(); // 로딩 시작

        ThemeRouteRequest request = new ThemeRouteRequest(themes);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getThemeRoutes(request).enqueue(new Callback<ThemeRouteResponse>() {
            @Override
            public void onResponse(Call<ThemeRouteResponse> call, Response<ThemeRouteResponse> response) {
                hideLoading(); // 로딩 종료

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("ThemeRouteActivity", "Raw JSON Response: " + new Gson().toJson(response.body()));

                    ThemeRouteResponse themeResponse = response.body();
                    if ("success".equals(themeResponse.getStatus())) {
                        List<ThemeRouteResponse.RouteData> routes = themeResponse.getData();
                        updateUI(routes);
                    } else {
                        Log.e("ThemeRouteActivity", "API 호출 실패: 상태 코드 " + themeResponse.getStatus());
                    }
                } else {
                    Log.e("ThemeRouteActivity", "API 호출 실패: 응답 실패");
                }
            }

            @Override
            public void onFailure(Call<ThemeRouteResponse> call, Throwable t) {
                hideLoading(); // 로딩 종료
                Log.e("ThemeRouteActivity", "API 호출 실패", t);
            }
        });
    }


    private void updateUI(List<ThemeRouteResponse.RouteData> routes) {
        // UI 컨테이너 참조
        LinearLayout routeContainer = findViewById(R.id.route_area);

        // 전달받은 버튼 이름 확인
        String buttonName = getIntent().getStringExtra("buttonName");

        // TextView 참조
        TextView themeDescriptionTextView = findViewById(R.id.txt_tip);

        // 테마별 설명 설정
        if ("유명 핫플레이스".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_movie_spots);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("역사적 장소".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_historic_places);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("자전거 여행 명소".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_bike_trails);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("도보 20마일 여행".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_walking_trails);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("사진촬영 명소".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_photo_spots);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("유명, 분위기 좋은 카페들".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_cafes);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("겨울여행 필수 여행지".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_winter_destinations);
            themeDescriptionTextView.setText(themeDescription);
        } else if ("레저, 캠핑".equals(buttonName)) {
            String themeDescription = getString(R.string.theme_leisure_camping);
            themeDescriptionTextView.setText(themeDescription);
        } else {
            themeDescriptionTextView.setText("해당하는 테마 설명이 없습니다.");
        }

        for (ThemeRouteResponse.RouteData route : routes) {
            // XML 레이아웃을 동적으로 inflate
            View routeView = LayoutInflater.from(this).inflate(R.layout.inc_route_ui, routeContainer, false);

            // 유저 이름
            TextView userNameText = routeView.findViewById(R.id.user_name_text);
            userNameText.setText(route.getUser_name());

            // 추천 문구
            TextView recommendationText = routeView.findViewById(R.id.user_detail);
            recommendationText.setText(route.getTheme_name());

            // 방문 여행지 수
            TextView visitedPlacesText = routeView.findViewById(R.id.route_count_text);
            visitedPlacesText.setText("방문 여행지 | " + route.getRoute_details().size());

            // 이동 거리
            TextView distanceText = routeView.findViewById(R.id.route_distance_text);
            distanceText.setText("이동거리 | " + String.format("%.1f km", route.getTotal_distance()));

            // 예상 경비
            TextView expenseText = routeView.findViewById(R.id.route_expense_text);
            expenseText.setText("예상경비 | " + String.format("%,.0f 원", route.getEstimated_cost()));

            // **Base64 이미지를 디코딩하여 ImageView에 표시**
            ImageView routeImageView = routeView.findViewById(R.id.route_image);
            String base64Image = route.getRouteImage(); // Base64 문자열 가져오기
            if (base64Image != null && !base64Image.isEmpty()) {
                Bitmap bitmap = decodeBase64Image(base64Image);
                routeImageView.setImageBitmap(bitmap);
            } else {
                routeImageView.setImageResource(R.drawable.img_image_notfound); // 기본 이미지
            }



            // 숨겨진 데이터 저장: RouteData 객체를 태그에 저장
            routeView.setTag(route);

            // 클릭 이벤트 설정
            routeView.setOnClickListener(v -> {
                // 저장된 RouteData 객체 가져오기
                ThemeRouteResponse.RouteData clickedRoute = (ThemeRouteResponse.RouteData) v.getTag();

                // 클릭된 항목의 정보를 로그로 출력
                Log.d("ThemeRouteActivity", "Clicked Route Info:");
                Log.d("ThemeRouteActivity", "Route ID: " + clickedRoute.getRoute_id());
                Log.d("ThemeRouteActivity", "User Name: " + clickedRoute.getUser_name());
                Log.d("ThemeRouteActivity", "Theme Name: " + clickedRoute.getTheme_name());
                Log.d("ThemeRouteActivity", "Visited Places: " + clickedRoute.getRoute_details().size());
                Log.d("ThemeRouteActivity", "Total Distance: " + clickedRoute.getTotal_distance() + " km");
                Log.d("ThemeRouteActivity", "Estimated Cost: " + clickedRoute.getEstimated_cost() + " 원");
                Log.d("ThemeRouteActivity", "Areas: " + clickedRoute.getAreas());
                Log.d("ThemeRouteActivity", "Tags: " + clickedRoute.getTags());
                Log.d("ThemeRouteActivity", "Image URL: " + route.getRouteImage());

                // 여행지의 세부 장소 정보 출력
                if (clickedRoute.getRoute_details() != null && !clickedRoute.getRoute_details().isEmpty()) {
                    for (ThemeRouteResponse.RouteDetail detail : clickedRoute.getRoute_details()) {
                        Log.d("ThemeRouteActivity", "Place Name: " + detail.getPlace_name());
                        Log.d("ThemeRouteActivity", "Latitude: " + detail.getLatitude());
                        Log.d("ThemeRouteActivity", "Longitude: " + detail.getLongitude());
                        Log.d("ThemeRouteActivity", "Date: " + detail.getDate());
                        Log.d("ThemeRouteActivity", "Sequence: " + detail.getSequence());
                    }
                } else {
                    Log.d("ThemeRouteActivity", "No route details available for this route.");
                }
            });

            // 동적으로 생성된 뷰를 컨테이너에 추가
            routeContainer.addView(routeView);


        }
    }
    // Base64 문자열을 Bitmap으로 디코딩하는 메서드
    private Bitmap decodeBase64Image(String base64String) {
        byte[] decodedString = android.util.Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
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
