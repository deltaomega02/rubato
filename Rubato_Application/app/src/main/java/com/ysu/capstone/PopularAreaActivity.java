package com.ysu.capstone;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.NaverApiClient;
import com.ysu.capstone.network.NaverApiService;
import com.ysu.capstone.network.NaverImageResponse;
import com.ysu.capstone.network.PlaceRequest;
import com.ysu.capstone.network.PlaceResponse;
import com.ysu.capstone.network.RetrofitClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PopularAreaActivity extends AppCompatActivity {
    private static final String TAG = "PopularAreaActivity";
    private List<Call<?>> ongoingCalls = new ArrayList<>();
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_area);

        // 뒤로가기 버튼 초기화
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // ProgressBar 초기화
        loadingProgressBar = findViewById(R.id.loading_progress);

        // Intent에서 지역명 받아오기
        String areaName = getIntent().getStringExtra("area_name");
        Log.d(TAG, "전달받은 지역명: " + areaName);

        // 지역명 TextView 찾아서 설정
        TextView areaNameTextView = findViewById(R.id.area_name);
        if (areaNameTextView != null && areaName != null) {
            areaNameTextView.setText(areaName);
        }

        setAreaTip(areaName); // 지역 팁 설정


        // 관광지 리스트 가져오기
        if (areaName != null) {
            getPlacesForArea(areaName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 진행 중인 모든 네트워크 요청 취소
        for (Call<?> call : ongoingCalls) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }

        // 리스트를 비워줌
        ongoingCalls.clear();
    }

    private void getPlacesForArea(String areaName) {
        showLoading(); // 로딩 시작

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        PlaceRequest request = new PlaceRequest(List.of(areaName));

        Call<PlaceResponse> call = apiService.getPlaces(request);

        ongoingCalls.add(call);

        call.enqueue(new Callback<PlaceResponse>() {
            @Override
            public void onResponse(Call<PlaceResponse> call, Response<PlaceResponse> response) {
                ongoingCalls.remove(call);
                hideLoading(); // 로딩 종료

                if (response.isSuccessful() && response.body() != null) {
                    updateUIWithPlaces(response.body());
                } else {
                    Log.e(TAG, "API 호출 실패: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PlaceResponse> call, Throwable t) {
                ongoingCalls.remove(call);
                hideLoading(); // 로딩 종료
                Log.e(TAG, "API 호출 실패", t);
            }
        });
    }

    private void updateUIWithPlaces(PlaceResponse placeResponse) {
        // UI 컨테이너 참조
        LinearLayout placeContainer = findViewById(R.id.route_area);

        List<String> placeNames = placeResponse.getPlaceNames();
        List<String> placeAddresses = placeResponse.getPlaceAddresses();
        List<Double> latitudes = placeResponse.getLatitudes();
        List<Double> longitudes = placeResponse.getLongitudes();

        // 지역명 가져오기 (예: 인텐트로 전달받은 지역명)
        String regionName = getIntent().getStringExtra("area_name");

        if (placeNames == null || placeNames.isEmpty()) {
            Log.d(TAG, "No places found for this area.");
            return;
        }

        // 동적으로 UI 생성
        for (int i = 0; i < placeNames.size(); i++) {
            String name = placeNames.get(i);
            String address = placeAddresses != null && placeAddresses.size() > i ? placeAddresses.get(i) : "주소 정보 없음";
            Double latitude = latitudes != null && latitudes.size() > i ? latitudes.get(i) : null;
            Double longitude = longitudes != null && longitudes.size() > i ? longitudes.get(i) : null;

            // XML 레이아웃을 동적으로 inflate
            View placeView = LayoutInflater.from(this).inflate(R.layout.inc_place_ui, placeContainer, false);

            // 텍스트 설정
            TextView placeNameText = placeView.findViewById(R.id.user_name_text);
            placeNameText.setText(name);

            TextView placeAddressText = placeView.findViewById(R.id.user_detail);
            placeAddressText.setText(address);

            ImageView placeImageView = placeView.findViewById(R.id.place_image);

            // 지역명 + 장소명으로 검색어 생성하여 이미지 로드
            if (regionName != null) {
                searchImageForPlace(regionName, name, placeImageView);
            } else {
                Log.e(TAG, "Region name is null. Cannot perform image search.");
            }

            // 클릭 이벤트 설정 (예: 상세 보기로 이동)
            placeView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked Place - Name: " + name + ", Lat: " + latitude + ", Lng: " + longitude);
                // 관광지 상세 보기로 이동 (선택 사항)
            });

            // 동적으로 생성된 뷰를 컨테이너에 추가
            placeContainer.addView(placeView);
        }
    }

    private void searchImageForPlace(String regionName, String placeName, ImageView imageView) {
        String query = regionName + " " + placeName;
        NaverApiService naverApiService = NaverApiClient.getApiService();

        showLoading(); // 로딩 시작

        Call<NaverImageResponse> call = naverApiService.searchImages(
                BuildConfig.NAVER_CLIENT_ID,
                BuildConfig.NAVER_CLIENT_SECRET,
                query,
                1,
                1,
                "sim"
        );

        ongoingCalls.add(call);

        call.enqueue(new Callback<NaverImageResponse>() {
            @Override
            public void onResponse(Call<NaverImageResponse> call, Response<NaverImageResponse> response) {
                ongoingCalls.remove(call);
                hideLoading(); // 로딩 종료

                if (isDestroyed() || isFinishing()) {
                    Log.w(TAG, "Activity destroyed, skipping image load.");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<NaverImageResponse.ImageItem> items = response.body().getItems();
                    if (!items.isEmpty()) {
                        String imageUrl = items.get(0).getLink();
                        Glide.with(PopularAreaActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.img_image_notfound)
                                .into(imageView);
                    }
                } else {
                    Log.e(TAG, "Response failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<NaverImageResponse> call, Throwable t) {
                ongoingCalls.remove(call);
                hideLoading(); // 로딩 종료

                if (isDestroyed() || isFinishing()) {
                    Log.w(TAG, "Activity destroyed, skipping failure handling.");
                    return;
                }

                Log.e(TAG, "Image search failed", t);
            }
        });
    }



    private void setAreaTip(String areaName) {
        TextView areaTipTextView = findViewById(R.id.txt_tip);
        String[] areaTips = getResources().getStringArray(R.array.area_tip);

        switch (areaName) {
            case "속초":
                areaTipTextView.setText(areaTips[0]);
                break;
            case "부산":
                areaTipTextView.setText(areaTips[1]);
                break;
            case "제주도":
                areaTipTextView.setText(areaTips[2]);
                break;
            case "서울":
                areaTipTextView.setText(areaTips[3]);
                break;
            default:
                areaTipTextView.setText("해당 지역에 대한 팁이 없습니다.");
                break;
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
