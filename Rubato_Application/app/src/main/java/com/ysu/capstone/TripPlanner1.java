package com.ysu.capstone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.ysu.capstone.decorators.DotLoadingAnimation;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.PlaceRequest;
import com.ysu.capstone.network.PlaceResponse;
import com.ysu.capstone.network.RetrofitClient;
import com.ysu.capstone.network.RecommentRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class TripPlanner1 extends AppCompatActivity {
    private ImageView back;

    private ConstraintLayout topSheet;
    private Flow checkboxFlow;
    private LinearLayout selectedAreaList;
    private LinearLayout selectedRegionsContainer;
    private ConstraintLayout searchArea;
    private ImageView topSheetBar;
    private List<String> selectedLocations = new ArrayList<>();
    private Handler handler = new Handler(); // Handler를 클래스 변수로 선언
    private boolean isAnimatingText = false; // 텍스트 애니메이션 여부 확인

    private View scrollView;
    private ImageView dividerLine;
    private ConstraintLayout selectedArea;
    private Flow significantFlow;

    //수정된 탑시트
    private float startY;
    private float expandedY;
    private boolean isExpanded = false;
    private static final float SWIPE_THRESHOLD = 100;

    //검색창
    private List<String> originalLocationNames; // 원본 데이터
    private List<String> filteredLocationNames; // 필터링된 데이터

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_planner1);


        // 화면의 기본 뷰들 연결
        topSheet = findViewById(R.id.top_sheet);
        checkboxFlow = findViewById(R.id.checkbox_flow);
        selectedAreaList = findViewById(R.id.selected_area_list);
        selectedRegionsContainer = findViewById(R.id.selected_regions_container);
        searchArea = findViewById(R.id.top_search);
        topSheetBar = findViewById(R.id.top_sheet_bar);
        findViewById(R.id.checkbox_flow).setVisibility(View.GONE);

        // 추가 뷰 요소들 연결
        scrollView = findViewById(R.id.scrollView);
        dividerLine = findViewById(R.id.divider_line);
        selectedArea = findViewById(R.id.selected_area);

        // 상단 시트 설정 추가
        setupSheet();


        //검색창
        // location_names 배열 초기화
        originalLocationNames = Arrays.asList(getResources().getStringArray(R.array.location_names));
        filteredLocationNames = new ArrayList<>(originalLocationNames); // 초기 상태는 전체 데이터

        EditText searchText = findViewById(R.id.search_text);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 필요 시 구현
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLocations(s.toString()); // 입력값에 따라 데이터 필터링
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 필요 시 구현
            }
        });

        addDynamicLocations(); // 초기 UI 생성

        // 상단 시트 그림자 설정
        topSheet.setElevation(20f);

        // 태그와 지역 목록 동적 생성
        addDynamicTags();
        addDynamicLocations();


        // 상단 시트를 최상위로 올림
        topSheet.bringToFront();

        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("places_loaded", false)
                .apply();

        // 지역 선택 완료 버튼 클릭 시 다음 화면으로 이동
        TextView btnSelected = findViewById(R.id.btn_selected);
        btnSelected.setOnClickListener(v -> {
            fetchPlaces(); // 선택한 지역 리스트 서버 전송
            Intent intent = new Intent(TripPlanner1.this, TripPlanner2.class);
            intent.putStringArrayListExtra("selectedLocations", (ArrayList<String>) selectedLocations);
            intent.putStringArrayListExtra("selectedTags", (ArrayList<String>) getSelectedTags());  // 태그 추가
            startActivity(intent);
        });

        // 뒤로가기 버튼 설정
        ImageView backButton = findViewById(R.id.ic_back);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(TripPlanner1.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    //검색창
    private void filterLocations(String query) {
        filteredLocationNames.clear();
        if (query.isEmpty()) {
            // 검색어가 없으면 전체 데이터 표시
            filteredLocationNames.addAll(originalLocationNames);
        } else {
            // 검색어가 포함된 항목만 추가
            for (String location : originalLocationNames) {
                if (location.toLowerCase().contains(query.toLowerCase())) {
                    filteredLocationNames.add(location);
                }
            }
        }
        refreshLocationList(); // 필터링된 데이터로 UI 갱신
    }






    private void fetchPlaces() {
        // 선택된 지역 저장
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("selected_locations", new Gson().toJson(selectedLocations))
                .apply();

        Log.d("TripPlanner1", "전송할 지역 리스트: " + new Gson().toJson(new PlaceRequest(selectedLocations)));

        // 백그라운드 스레드에서 API 호출
        new Thread(() -> {
            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            try {
                Response<PlaceResponse> response = apiService.getPlaces(
                        new PlaceRequest(selectedLocations)).execute();

                if (response.isSuccessful() && response.body() != null) {
                    PlaceResponse placeResponse = response.body();
                    if (placeResponse != null) {
                        // 데이터 저장 후 플래그를 true로 설정
                        saveResponseToCache(placeResponse);
                        prefs.edit()
                                .putBoolean("places_loaded", true)
                                .apply();

                        runOnUiThread(() -> {
                            Toast.makeText(TripPlanner1.this,
                                    "장소 정보를 성공적으로 가져왔습니다.", Toast.LENGTH_SHORT).show();
                        });

                        // 파싱된 응답 데이터 상세 로깅
                        Log.d("TripPlanner1", "파싱된 응답 - place_names: " +
                                (placeResponse.getPlaceNames() != null ?
                                        placeResponse.getPlaceNames().toString() : "null"));
                        Log.d("TripPlanner1", "파싱된 응답 - place_addresses: " +
                                (placeResponse.getPlaceAddresses() != null ?
                                        placeResponse.getPlaceAddresses().toString() : "null"));
                        Log.d("TripPlanner1", "파싱된 응답 - latitudes: " +
                                (placeResponse.getLatitudes() != null ?
                                        placeResponse.getLatitudes().toString() : "null"));
                        Log.d("TripPlanner1", "파싱된 응답 - longitudes: " +
                                (placeResponse.getLongitudes() != null ?
                                        placeResponse.getLongitudes().toString() : "null"));
                        Log.d("TripPlanner1", "파싱된 응답 - place_type: " +
                                (placeResponse.getPlaceType() != null ?
                                        placeResponse.getPlaceType().toString() : "null"));
                        Log.d("TripPlanner1", "파싱된 응답 - tags: " +
                                (placeResponse.getTags() != null ?
                                        placeResponse.getTags().toString() : "null"));

                        // 캐시에 저장
                        saveResponseToCache(placeResponse);
                    } else {
                        Log.e("TripPlanner1", "응답 바디가 null입니다");
                        runOnUiThread(() -> {
                            Toast.makeText(TripPlanner1.this,
                                    "서버 응답이 비어있습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    String errorBody = response.errorBody() != null ?
                            response.errorBody().string() : "Unknown error";
                    Log.e("TripPlanner1", "지역 리스트 전송 실패. 응답 코드: " +
                            response.code() + ", 에러 응답: " + errorBody);
                    runOnUiThread(() -> {
                        Toast.makeText(TripPlanner1.this,
                                "서버 응답 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("TripPlanner1", "서버 전송 오류: " + e.getMessage());
                Log.e("TripPlanner1", "상세 에러: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(TripPlanner1.this,
                            "서버 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 지역 선택 시 recomment_area 애니메이션 시작 및 위치 조정 코드 추가
    private void sendLocationRecommentRequest(String locationName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<ResponseBody> call = apiService.locationRecomment(new RecommentRequest(locationName));

        TextView recommentTextView = findViewById(R.id.recomment);
        ConstraintLayout recommentArea = findViewById(R.id.recomment_area);
        LinearLayout dotLoadingAnimation = findViewById(R.id.dot_loading_animation);

        // 기존 텍스트와 핸들러 초기화
        handler.removeCallbacksAndMessages(null); // 이전 핸들러 작업 취소
        recommentTextView.setText(""); // 기존 텍스트 초기화

        // 로딩 메시지와 애니메이션 표시
        recommentTextView.setText("메시지가 작성중이에요");
        recommentArea.setVisibility(View.VISIBLE);
        dotLoadingAnimation.setVisibility(View.VISIBLE);
        startDotLoadingAnimation();

        // recommentArea가 이미 위로 올라와 있는지 확인하여 애니메이션 조건 처리
        if (recommentArea.getTranslationY() != 0) {
            slideUpRecommentArea(recommentArea); // 올라가 있지 않으면 슬라이드 업
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                stopDotLoadingAnimation();
                dotLoadingAnimation.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseText = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseText);
                        JSONArray choicesArray = jsonResponse.getJSONArray("choices");
                        JSONObject choiceObject = choicesArray.getJSONObject(0);
                        JSONObject messageObject = choiceObject.getJSONObject("message");
                        String content = messageObject.getString("content");

                        animateText(content, recommentTextView); // 텍스트 애니메이션

                        // 텍스트가 변경된 후 recomment_area 위치 조정
                        recommentTextView.post(() -> adjustRecommentArea(recommentArea, recommentTextView));
                    } catch (Exception e) {
                        Log.e("TripPlanner1", "응답 파싱 오류: " + e.getMessage());
                        recommentTextView.setText("응답 파싱 오류가 발생했습니다.");
                    }
                } else {
                    Log.e("TripPlanner1", "서버 오류: " + response.code());
                    recommentTextView.setText("서버 오류가 발생했습니다.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                stopDotLoadingAnimation();
                dotLoadingAnimation.setVisibility(View.GONE);
                Log.e("TripPlanner1", "API 요청 실패: " + t.getMessage());
                recommentTextView.setText("API 요청 실패");
            }
        });
    }

    private final SparseArray<Integer> logoResourceCache = new SparseArray<>();
    private final SparseArray<Integer> typeIconCache = new SparseArray<>();

    ///////////////////////////////////////////////////////////////////

    // recomment_area가 화면 아래에서 130dp 추가된 위치에서 위로 슬라이드하며 나타나도록 설정하는 메서드
    private void slideUpRecommentArea(View recommentArea) {
        // 화면 높이 가져오기
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 130dp를 픽셀 단위로 변환
        float additionalOffset = 530 / getResources().getDisplayMetrics().density;

        // recommentArea의 초기 위치를 화면 아래 + 130dp로 설정
        recommentArea.setTranslationY(screenHeight + additionalOffset);
        recommentArea.setVisibility(View.VISIBLE);

        // recommentArea를 위로 슬라이드하여 화면에 나타나도록 애니메이션 설정
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(recommentArea, "translationY", 0);
        slideUp.setDuration(300); // 슬라이드 속도
        slideUp.start();
    }

    /////////////////////////////////////////////////////////////////////////

    // recomment_area가 아래로 슬라이드하여 사라지도록 설정하는 메서드
    private void slideDownRecommentArea(View recommentArea) {
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(recommentArea, "translationY", recommentArea.getHeight());
        slideDown.setDuration(200);

        // 애니메이션 종료 후 뷰를 숨김
        slideDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recommentArea.setVisibility(View.GONE);
            }
        });

        slideDown.start();
    }

    // TextView 길이에 따라 recomment_area 위치를 조정하는 메서드
    private void adjustRecommentArea(View recommentArea, TextView recommentTextView) {
        recommentTextView.post(() -> {
            int lineCount = recommentTextView.getLineCount();
            int additionalOffset = lineCount > 1 ? (lineCount - 1) * recommentTextView.getLineHeight() : 0;

            recommentArea.setTranslationY(recommentArea.getTranslationY() - additionalOffset); // 추가로 위로 이동
        });
    }




    //////////////////////한단어씩 나오도록 하기////////////////////////////
    private void animateText(String text, TextView textView) {
        handler.removeCallbacksAndMessages(null); // 이전 핸들러 작업 취소
        textView.setText(""); // 초기화

        String[] words = text.split(" "); // 텍스트를 단어 단위로 분리
        for (int i = 0; i < words.length; i++) {
            int index = i;
            handler.postDelayed(() -> {
                // 기존 텍스트에 한 단어 추가 (단어 사이에 공백 추가)
                textView.setText(textView.getText().toString() + (index > 0 ? " " : "") + words[index]);
            }, i * 150); // 500ms 간격으로 한 단어씩 추가
        }
    }

    /////////////////////로딩중 애니메이션////////////////////////////////
    // 애니메이션 시작 메서드
    private void startDotLoadingAnimation() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // DotLoadingAnimation 클래스를 사용하여 애니메이션 시작
        DotLoadingAnimation.start(dot1, dot2, dot3);
    }

    // 애니메이션 중지 메서드
    private void stopDotLoadingAnimation() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        dot1.clearAnimation();
        dot2.clearAnimation();
        dot3.clearAnimation();
    }

    // 선택 취소 시 호출할 메서드
    private void deselectLocation(String locationName) {
        selectedLocations.remove(locationName);
        slideDownRecommentArea(findViewById(R.id.recomment_area)); // 아래로 슬라이드하여 사라지기
        updateSelectedArea(); // 선택된 지역 UI 업데이트
    }


    private void saveResponseToCache(PlaceResponse placeResponse) {
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        List<String> names = placeResponse.getPlaceNames();
        List<String> addresses = placeResponse.getPlaceAddresses();
        List<Double> lats = placeResponse.getLatitudes();
        List<Double> lngs = placeResponse.getLongitudes();
        List<String> types = placeResponse.getPlaceType();
        List<List<String>> tags = placeResponse.getTags();  // 태그 정보 추가

        Log.d("TripPlanner1", "캐시 저장 전 데이터 확인:");
        Log.d("TripPlanner1", "Names: " + (names != null ? names.toString() : "null") +
                " (size: " + (names != null ? names.size() : 0) + ")");
        Log.d("TripPlanner1", "Addresses: " + (addresses != null ? addresses.toString() : "null") +
                " (size: " + (addresses != null ? addresses.size() : 0) + ")");
        Log.d("TripPlanner1", "Latitudes: " + (lats != null ? lats.toString() : "null") +
                " (size: " + (lats != null ? lats.size() : 0) + ")");
        Log.d("TripPlanner1", "Longitudes: " + (lngs != null ? lngs.toString() : "null") +
                " (size: " + (lngs != null ? lngs.size() : 0) + ")");
        Log.d("TripPlanner1", "Types: " + (types != null ? types.toString() : "null") +
                " (size: " + (types != null ? types.size() : 0) + ")");
        Log.d("TripPlanner1", "Tags: " + (tags != null ? tags.toString() : "null") +
                " (size: " + (tags != null ? tags.size() : 0) + ")");

        editor.putString("place_names", gson.toJson(names));
        editor.putString("place_addresses", gson.toJson(addresses));
        editor.putString("latitudes", gson.toJson(lats));
        editor.putString("longitudes", gson.toJson(lngs));
        editor.putString("place_type", gson.toJson(types));
        editor.putString("place_tags", gson.toJson(tags));  // 태그 저장
        editor.apply();

        // 저장 후 데이터 확인
        String savedNames = prefs.getString("place_names", null);
        String savedAddresses = prefs.getString("place_addresses", null);
        String savedLats = prefs.getString("latitudes", null);
        String savedLngs = prefs.getString("longitudes", null);
        String savedTypes = prefs.getString("place_type", null);
        String savedTags = prefs.getString("place_tags", null);  // 태그 확인

        Log.d("TripPlanner1", "캐시 저장 후 데이터 확인:");
        Log.d("TripPlanner1", "Saved Names: " + savedNames);
        Log.d("TripPlanner1", "Saved Addresses: " + savedAddresses);
        Log.d("TripPlanner1", "Saved Latitudes: " + savedLats);
        Log.d("TripPlanner1", "Saved Longitudes: " + savedLngs);
        Log.d("TripPlanner1", "Saved Types: " + savedTypes);
        Log.d("TripPlanner1", "Saved Tags: " + savedTags);
    }


    @Override
    protected void onResume() {
        super.onResume();
        topSheet.bringToFront();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setupSheet() {
        topSheet.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchY;
            private float initialTranslationY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchY = event.getRawY();
                        initialTranslationY = topSheet.getTranslationY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float currentTouchY = event.getRawY();
                        float deltaY = currentTouchY - initialTouchY;
                        float newTranslationY = initialTranslationY + deltaY;

                        // 이동 범위 제한 (-350dp ~ 0dp)로 수정
                        newTranslationY = Math.max(-convertDpToPx(410), Math.min(newTranslationY, 0));
                        topSheet.setTranslationY(newTranslationY);

                        findViewById(R.id.tag_scroll).setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float translationY = topSheet.getTranslationY();
                        if (Math.abs(translationY) < convertDpToPx(175)) {
                            expandSheet();
                        } else {
                            collapseSheet();
                        }
                        return true;
                }
                return false;
            }
        });

        // 기본 위치 설정 (-350dp)
        topSheet.setTranslationY(-convertDpToPx(410));

        findViewById(R.id.top_sheet_bar).setOnClickListener(v -> {
            if (isExpanded) {
                collapseSheet();
            } else {
                expandSheet();
            }
        });
    }

    private void expandSheet() {
        isExpanded = true;
        topSheet.animate()
                .translationY(0)
                .setDuration(300)
                .start();
    }

    private void collapseSheet() {
        isExpanded = false;
        topSheet.animate()
                .translationY(-convertDpToPx(410))  // -410dp에서 -350dp로 수정
                .setDuration(300)
                .start();
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////



    private void addDynamicTags() {
        String[] tags = getResources().getStringArray(R.array.tag_options);
        List<String> tagList = new ArrayList<>();
        Collections.addAll(tagList, tags);

        List<Integer> checkboxIds = new ArrayList<>();
        ScrollView tagScroll = findViewById(R.id.tag_scroll);

        // ScrollView 내부에 ConstraintLayout 생성
        ConstraintLayout flowContainer = new ConstraintLayout(this);
        flowContainer.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        // Flow 생성 및 설정
        Flow checkboxFlow = new Flow(this);
        checkboxFlow.setId(View.generateViewId());
        ConstraintLayout.LayoutParams flowParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        flowParams.setMargins(
                convertDpToPx(20),
                convertDpToPx(15),
                convertDpToPx(20),
                convertDpToPx(5)
        );
        checkboxFlow.setLayoutParams(flowParams);

        // Flow 속성 설정
        checkboxFlow.setWrapMode(Flow.WRAP_CHAIN);
        checkboxFlow.setHorizontalGap(convertDpToPx(10));
        checkboxFlow.setVerticalGap(convertDpToPx(10));
        checkboxFlow.setHorizontalStyle(Flow.CHAIN_PACKED);
        checkboxFlow.setHorizontalBias(0.5f);

        // ConstraintLayout에 Flow 추가
        flowContainer.addView(checkboxFlow);

        // ScrollView 내부 초기화
        tagScroll.removeAllViews();
        tagScroll.addView(flowContainer);

        // 체크박스 생성 및 추가
        for (String tag : tagList) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(tag);
            checkBox.setId(View.generateViewId());
            checkboxIds.add(checkBox.getId());

            // 체크박스 스타일링
            checkBox.setBackgroundResource(R.drawable.drawable_white_bordered_rectangle_100);
            checkBox.setPadding(
                    convertDpToPx(5),
                    convertDpToPx(5),
                    convertDpToPx(5),
                    convertDpToPx(5)
            );
            checkBox.setElevation(5f);



            // 레이아웃 파라미터 설정
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(
                    convertDpToPx(5),
                    convertDpToPx(5),
                    convertDpToPx(5),
                    convertDpToPx(5)
            );
            checkBox.setLayoutParams(layoutParams);

            flowContainer.addView(checkBox);
        }

        // Flow에 체크박스 ID 연결
        int[] checkboxIdArray = new int[checkboxIds.size()];
        for (int i = 0; i < checkboxIds.size(); i++) {
            checkboxIdArray[i] = checkboxIds.get(i);
        }
        checkboxFlow.setReferencedIds(checkboxIdArray);
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void addDynamicLocations() {
        // 지역 이름, 관광명소, 로고, 타입 배열 가져오기
        String[] locationNames = getResources().getStringArray(R.array.location_names);
        String[] popularSpots = getResources().getStringArray(R.array.popular_spots);
        String[] locationLogos = getResources().getStringArray(R.array.location_logo);
        String[] locationTypes = getResources().getStringArray(R.array.loc_type);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < locationNames.length; i++) {
            View itemView = inflater.inflate(R.layout.inc_location_item, selectedAreaList, false);

            TextView locationNameView = itemView.findViewById(R.id.location_name);
            TextView popularSpotView = itemView.findViewById(R.id.popular_spot);
            ImageView locationItem = itemView.findViewById(R.id.location_item);
            ImageView selectionIcon = itemView.findViewById(R.id.selection_icon);
            ImageView locationLogoImage = itemView.findViewById(R.id.location_logo_image);
            ImageView locationTypeIcon1 = itemView.findViewById(R.id.location_type_icon1);
            ImageView locationTypeIcon2 = itemView.findViewById(R.id.location_type_icon2);

            String locationName = locationNames[i];
            locationNameView.setText(locationName);
            popularSpotView.setText(popularSpots[i]);

            // 로고 설정
            int logoResId = getResources().getIdentifier(locationLogos[i], "drawable", getPackageName());
            if (logoResId != 0) {
                locationLogoImage.setImageResource(logoResId);
            } else {
                Log.e("TripPlanner1", "이미지 리소스를 찾을 수 없습니다: " + locationLogos[i]);
            }

            // locationTypes에서 타입을 가져와서 쉼표로 구분하고, 각 아이콘에 맞는 이미지를 할당
            String[] types = locationTypes[i].split(",");
            if (types.length > 0) {
                locationTypeIcon1.setImageResource(getLocationTypeIcon(types[0].trim()));
                locationTypeIcon1.setVisibility(View.VISIBLE);
            }
            if (types.length > 1) {
                locationTypeIcon2.setImageResource(getLocationTypeIcon(types[1].trim()));
                locationTypeIcon2.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (selectedLocations.contains(locationName)) {
                    deselectLocation(locationName);
                    selectionIcon.setImageResource(R.drawable.drawable_circle_unfilled);
                    locationItem.setImageResource(R.drawable.drawable_white_bordered_rectangle_100);
                    itemView.setElevation(20f);
                } else {
                    selectedLocations.add(locationName);
                    selectionIcon.setImageResource(R.drawable.ic_checked_circle);
                    locationItem.setImageResource(R.drawable.drawable_blue_bordered_rectangle_100);
                    itemView.setElevation(0f);
                    sendLocationRecommentRequest(locationName);
                }
                updateSelectedArea();
            });

            selectionIcon.setImageResource(selectedLocations.contains(locationName) ?
                    R.drawable.ic_checked_circle : R.drawable.drawable_circle_unfilled);
            selectedAreaList.addView(itemView);

        }
    }

    // 각 타입에 맞는 아이콘을 반환하는 메서드
    private int getLocationTypeIcon(String type) {
        switch (type) {
            case "도시":
                return R.drawable.loc_type_urban;
            case "시골":
                return R.drawable.loc_type_countryside;
            case "산":
                return R.drawable.loc_type_mountain;
            case "바다":
                return R.drawable.loc_type_sea;
            default:
                return 0;
        }
    }

    private List<String> getSelectedTags() {
        List<String> selectedTags = new ArrayList<>();
        ScrollView tagScroll = findViewById(R.id.tag_scroll);

        if (tagScroll.getChildCount() > 0) {
            View firstChild = tagScroll.getChildAt(0);
            if (firstChild instanceof ConstraintLayout) {
                ConstraintLayout container = (ConstraintLayout) firstChild;

                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        if (checkBox.isChecked()) {
                            selectedTags.add(checkBox.getText().toString());
                        }
                    }
                }
            }
        }

        return selectedTags;
    }



    private void updateSelectedArea() {
        // 기존에 선택된 지역 목록 초기화
        selectedRegionsContainer.removeAllViews();

        // XML 레이아웃을 View 객체로 변환하기 위한 인플레이터
        LayoutInflater inflater = LayoutInflater.from(this);

        // 지역 이름 및 로고 배열 가져오기
        String[] locationNames = getResources().getStringArray(R.array.location_names);
        String[] locationLogos = getResources().getStringArray(R.array.location_logo);

        // 선택된 지역 개수만큼 반복
        for (String locationName : selectedLocations) {
            // 선택된 지역 아이템 레이아웃 inflate
            View regionView = inflater.inflate(R.layout.inc_selected_region_item, selectedRegionsContainer, false);

            // 각 뷰 요소 연결
            ImageView regionImage = regionView.findViewById(R.id.region_image);
            ImageView regionLogoImage = regionView.findViewById(R.id.region_logo_image); // 추가한 로고 이미지 뷰
            TextView regionName = regionView.findViewById(R.id.region_name);
            ImageView removeIcon = regionView.findViewById(R.id.remove_icon);

            // 지역 이름 설정
            regionName.setText(locationName);

            // 선택된 지역 이름의 인덱스를 `location_names` 배열에서 찾아 로고 배열에서 해당 이미지 설정
            int locationIndex = Arrays.asList(locationNames).indexOf(locationName);
            if (locationIndex != -1 && locationIndex < locationLogos.length) {
                String logoResourceName = locationLogos[locationIndex];
                int logoResId = getResources().getIdentifier(logoResourceName, "drawable", getPackageName());
                if (logoResId != 0) {
                    regionLogoImage.setImageResource(logoResId);
                } else {
                    Log.e("TripPlanner1", "이미지 리소스를 찾을 수 없습니다: " + logoResourceName);
                }
            }

            // 삭제 버튼 클릭 이벤트 설정
            removeIcon.setOnClickListener(v -> {
                selectedLocations.remove(locationName);
                updateSelectedArea();
                refreshLocationList();
            });

            // 생성한 지역 아이템을 컨테이너에 추가
            selectedRegionsContainer.addView(regionView);

            // 마지막 아이템이 아니면 화살표 이미지 추가
            if (!locationName.equals(selectedLocations.get(selectedLocations.size() - 1))) {
                ImageView arrow = new ImageView(this);
                arrow.setImageResource(R.drawable.ic_double_arrow);

                // 화살표 이미지 크기와 여백 설정
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        80,
                        80
                );
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMargins(8, 0, 8, 0);
                arrow.setLayoutParams(params);
                selectedRegionsContainer.addView(arrow);
            }
        }

        // 선택 완료 버튼 텍스트 업데이트
        TextView btnSelected = findViewById(R.id.btn_selected);
        if (selectedLocations.size() > 1) {
            btnSelected.setText(selectedLocations.get(0) + " 외 " + (selectedLocations.size() - 1) + "개 선택 완료");
        } else if (selectedLocations.size() == 1) {
            btnSelected.setText(selectedLocations.get(0) + " 선택 완료");
        } else {
            btnSelected.setText("지역을 선택하세요");
        }
    }


    private void refreshLocationList() {
        selectedAreaList.removeAllViews(); // 기존 아이템 제거

        // 지역 이름, 관광명소, 로고, 타입 배열 가져오기
        String[] locationNames = getResources().getStringArray(R.array.location_names);
        String[] popularSpots = getResources().getStringArray(R.array.popular_spots);
        String[] locationLogos = getResources().getStringArray(R.array.location_logo);
        String[] locationTypes = getResources().getStringArray(R.array.loc_type);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String location : filteredLocationNames) {
            // location_names 배열에서 현재 필터된 위치의 인덱스를 찾음
            int locationIndex = Arrays.asList(locationNames).indexOf(location);

            if (locationIndex != -1) {
                // 레이아웃 인플레이트
                View itemView = inflater.inflate(R.layout.inc_location_item, selectedAreaList, false);

                TextView locationNameView = itemView.findViewById(R.id.location_name);
                TextView popularSpotView = itemView.findViewById(R.id.popular_spot);
                ImageView selectionIcon = itemView.findViewById(R.id.selection_icon);
                ImageView locationLogoImage = itemView.findViewById(R.id.location_logo_image);
                ImageView locationTypeIcon1 = itemView.findViewById(R.id.location_type_icon1);
                ImageView locationTypeIcon2 = itemView.findViewById(R.id.location_type_icon2);

                // 데이터 설정
                locationNameView.setText(location);
                popularSpotView.setText(popularSpots[locationIndex]);

                // 로고 설정
                int logoResId = getResources().getIdentifier(locationLogos[locationIndex], "drawable", getPackageName());
                if (logoResId != 0) {
                    locationLogoImage.setImageResource(logoResId);
                } else {
                    Log.e("TripPlanner1", "이미지 리소스를 찾을 수 없습니다: " + locationLogos[locationIndex]);
                }

                // 타입 아이콘 설정
                String[] types = locationTypes[locationIndex].split(",");
                if (types.length > 0) {
                    locationTypeIcon1.setImageResource(getLocationTypeIcon(types[0].trim()));
                    locationTypeIcon1.setVisibility(View.VISIBLE);
                }
                if (types.length > 1) {
                    locationTypeIcon2.setImageResource(getLocationTypeIcon(types[1].trim()));
                    locationTypeIcon2.setVisibility(View.VISIBLE);
                }

                // 클릭 이벤트
                itemView.setOnClickListener(v -> {
                    if (selectedLocations.contains(location)) {
                        deselectLocation(location);
                        selectionIcon.setImageResource(R.drawable.drawable_circle_unfilled);
                        itemView.setElevation(20f);
                    } else {
                        selectedLocations.add(location);
                        selectionIcon.setImageResource(R.drawable.ic_checked_circle);
                        itemView.setElevation(0f);
                        sendLocationRecommentRequest(location);
                    }
                    updateSelectedArea();
                });

                // 선택 상태 아이콘 설정
                selectionIcon.setImageResource(selectedLocations.contains(location) ?
                        R.drawable.ic_checked_circle : R.drawable.drawable_circle_unfilled);

                selectedAreaList.addView(itemView);
            }
        }
    }



}