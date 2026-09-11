package com.ysu.capstone;

// Android 기본 컴포넌트
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX 컴포넌트
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

// 외부 라이브러리
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Java 유틸리티
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripPlanner1_1 extends AppCompatActivity {


    private Flow checkboxFlow;
    private LinearLayout selectedPlaceList;
    private LinearLayout selectedPlacesContainer;
    private ArrayList<String> selectedPlaces = new ArrayList<>();
    private ArrayList<String> selectedAddresses = new ArrayList<>();
    private ArrayList<String> selectedType = new ArrayList<>();
    private ArrayList<Double> placeLatitudes = new ArrayList<>();
    private ArrayList<Double> placeLongitudes = new ArrayList<>();
    private ArrayList<List<String>> selectedTags = new ArrayList<>();  // 선택된 장소들의 태그 리스트
    private List<String> selectedTagFilters = new ArrayList<>(); // 선택된 태그 필터를 관리하는 리스트
    private Spinner sortSpinner;
    private Flow significantFlow;
    private TextView btnSelected;
    private String apiKeyId;
    private String apiKey;
    private ImageView back;

    private static final int MAX_RETRY_COUNT = 3;
    private int currentRetryCount = 0;

    private ConstraintLayout topSheet;
    private float startY;
    private float expandedY; // 펼쳐진 상태의 Y 위치
    private boolean isExpanded = false; // 처음에는 접힌 상태
    private static final float SWIPE_THRESHOLD = 100;


    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 생명주기 메서드_1 :: 액티비티 초기화 및 UI 구성 메서드
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_planner1_1);

        //정렬스피너
        sortSpinner = findViewById(R.id.sort_spinner);
        if (sortSpinner == null) {
            Log.e("TripPlanner1_1", "sortSpinner is null. Check XML ID or layout.");
            return;
        }

        //탑시트
        topSheet = findViewById(R.id.top_sheet);
        setupSheet();

        findViewById(R.id.checkbox_flow).setVisibility(View.GONE);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //검색 필터링
        EditText searchText = findViewById(R.id.search_text); // 검색 텍스트

        // 검색 텍스트 입력 감지
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 필요하면 구현
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 텍스트 변경 시 호출
                filterPlacesBySearchText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 필요하면 구현
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////


        // Naver API 키 초기화
        apiKeyId = getString(R.string.naver_api_key_id);
        apiKey = getString(R.string.naver_api_key);

        // UI 요소 초기화
        topSheet = findViewById(R.id.top_sheet);
        if (topSheet == null) {
            Log.e("TopSheet", "탑시트가 null입니다ㅁ. Check XML ID or layout.");
            return; // 초기화 실패 시 실행 중단
        }

// 뒤로가기 버튼 리스너
        back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            // 현재 액티비티 종료
            finish();
        });


        checkboxFlow = findViewById(R.id.checkbox_flow);
        selectedPlaceList = findViewById(R.id.selected_area_list);
        selectedPlacesContainer = findViewById(R.id.selected_regions_container);

        // 이동수단 스피너 설정
        String[] vehicles = getResources().getStringArray(R.array.tag_vehicles);
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicles);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        // 정렬 스피너 설정
        String[] sorts = getResources().getStringArray(R.array.sort_type);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sorts);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSort = parent.getItemAtPosition(position).toString();
                SharedPreferences sharedPreferences = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
                Gson gson = new Gson();

                // 데이터를 SharedPreferences에서 가져오기
                String placeNamesJson = sharedPreferences.getString("place_names", null);
                String placeAddressesJson = sharedPreferences.getString("place_addresses", null);
                String placeTypesJson = sharedPreferences.getString("place_type", null);

                if (placeNamesJson == null || placeAddressesJson == null || placeTypesJson == null) {
                    Toast.makeText(TripPlanner1_1.this, "장소 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // JSON 데이터를 ArrayList로 변환
                ArrayList<String> placeNames = gson.fromJson(placeNamesJson, new TypeToken<ArrayList<String>>() {}.getType());
                ArrayList<String> placeAddresses = gson.fromJson(placeAddressesJson, new TypeToken<ArrayList<String>>() {}.getType());
                ArrayList<String> placeTypes = gson.fromJson(placeTypesJson, new TypeToken<ArrayList<String>>() {}.getType());

                if (selectedSort.equals("가나다순")) {
                    Log.d("Sorting", "가나다순 정렬 시작");

                    // JSON 데이터를 ArrayList로 변환 시 모든 데이터를 가져오도록 수정
                    String latitudesJson = sharedPreferences.getString("latitudes", null);
                    String longitudesJson = sharedPreferences.getString("longitudes", null);
                    String placeTagsJson = sharedPreferences.getString("place_tags", null);

                    ArrayList<Double> latitudes = gson.fromJson(latitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
                    ArrayList<Double> longitudes = gson.fromJson(longitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
                    ArrayList<List<String>> placeTags = gson.fromJson(placeTagsJson, new TypeToken<ArrayList<List<String>>>() {}.getType());

                    // 인덱스 리스트로 정렬
                    List<Integer> indices = new ArrayList<>();
                    for (int i = 0; i < placeNames.size(); i++) {
                        indices.add(i);
                    }

                    Collator collator = Collator.getInstance();
                    collator.setStrength(Collator.PRIMARY);
                    Collections.sort(indices, (a, b) -> collator.compare(placeNames.get(a), placeNames.get(b)));

                    // 정렬된 데이터를 담을 리스트
                    ArrayList<String> sortedNames = new ArrayList<>();
                    ArrayList<String> sortedAddresses = new ArrayList<>();
                    ArrayList<String> sortedTypes = new ArrayList<>();
                    ArrayList<Double> sortedLatitudes = new ArrayList<>();
                    ArrayList<Double> sortedLongitudes = new ArrayList<>();
                    ArrayList<List<String>> sortedTags = new ArrayList<>();

                    for (int index : indices) {
                        sortedNames.add(placeNames.get(index));
                        sortedAddresses.add(placeAddresses.get(index));
                        sortedTypes.add(placeTypes.get(index));
                        sortedLatitudes.add(latitudes.get(index));
                        sortedLongitudes.add(longitudes.get(index));
                        sortedTags.add(placeTags.get(index));
                    }

                    // UI 업데이트 시 모든 데이터 전달
                    updatePlaceList(sortedNames, sortedAddresses, sortedLatitudes, sortedLongitudes, sortedTypes, sortedTags);
                    Log.d("Sorting", "가나다순 정렬 완료");
                } else if (selectedSort.equals("유형별 정렬")) {
                    Log.d("Sorting", "유형별 정렬 시작");

                    // 인덱스 리스트 생성
                    List<Integer> indices = new ArrayList<>();
                    for (int i = 0; i < placeTypes.size(); i++) {
                        indices.add(i);
                    }

                    // 유형별 정렬
                    Collections.sort(indices, (a, b) -> placeTypes.get(a).compareTo(placeTypes.get(b)));

                    // 정렬된 데이터를 담을 리스트
                    ArrayList<String> sortedNames = new ArrayList<>();
                    ArrayList<String> sortedAddresses = new ArrayList<>();
                    ArrayList<String> sortedTypes = new ArrayList<>();

                    for (int index : indices) {
                        sortedNames.add(placeNames.get(index));
                        sortedAddresses.add(placeAddresses.get(index));
                        sortedTypes.add(placeTypes.get(index));
                    }

                    // UI 업데이트
                    selectedPlaceList.removeAllViews();
                    updatePlaceList(sortedNames, sortedAddresses, new ArrayList<>(), new ArrayList<>(), sortedTypes, new ArrayList<>());
                    Log.d("Sorting", "유형별 정렬 완료");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무 작업도 필요하지 않음
            }

        });

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            ArrayList<String> previouslySelectedPlaces = receivedIntent.getStringArrayListExtra("previouslySelectedPlaces");
            ArrayList<String> previouslySelectedAddresses = receivedIntent.getStringArrayListExtra("previouslySelectedAddresses");
            ArrayList<String> previousLatitudes = receivedIntent.getStringArrayListExtra("previouslySelectedLatitudes");
            ArrayList<String> previousLongitudes = receivedIntent.getStringArrayListExtra("previouslySelectedLongitudes");

            int receivedCurrentDay = receivedIntent.getIntExtra("currentDay", -1);
            Log.d("TripPlanner1_1", "받은 currentDay 값: " + receivedCurrentDay);

            if (previouslySelectedPlaces != null && !previouslySelectedPlaces.isEmpty()) {
                // 기존 리스트들 초기화
                selectedPlaces.clear();
                selectedAddresses.clear();
                placeLatitudes.clear();
                placeLongitudes.clear();

                // 이전 선택 데이터 복원
                selectedPlaces.addAll(previouslySelectedPlaces);
                selectedAddresses.addAll(previouslySelectedAddresses);

                // 위도/경도 변환 및 추가
                for (int i = 0; i < previousLatitudes.size(); i++) {
                    placeLatitudes.add(Double.parseDouble(previousLatitudes.get(i)));
                    placeLongitudes.add(Double.parseDouble(previousLongitudes.get(i)));
                }

                // 거리 정보 초기화 및 재계산 준비
                distanceMap.clear();
                for (int i = 0; i < selectedPlaces.size() - 1; i++) {
                    String distanceKey = i + "_" + (i + 1);
                    distanceMap.put(distanceKey, new DistanceInfo("계산 중...", true));
                }

                // 선택된 장소 업데이트
                updateSelectedPlaces();
            }
        }

        addDynamicTags();
        addDynamicPlaces();

        // UI 엘리먼트 그림자 및 위치 설정
        topSheet.setElevation(20f);
        topSheet.bringToFront();

        btnSelected = findViewById(R.id.btn_selected);
        btnSelected.setOnClickListener(v -> {
            boolean isCalculating = false;
            for (int i = 0; i < selectedPlaces.size() - 1; i++) {
                String distanceKey = i + "_" + (i + 1);
                DistanceInfo distanceInfo = distanceMap.get(distanceKey);
                if (distanceInfo == null || distanceInfo.isCalculating) {
                    isCalculating = true;
                    break;
                }
            }

            if (isCalculating) {
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                findViewById(android.R.id.content).startAnimation(shake);

                Snackbar.make(v, "거리 계산이 진행 중입니다. 잠시만 기다려주세요.", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.skyblue))
                        .setTextColor(Color.WHITE)
                        .show();
                return;
            }

            Intent sendIntent = new Intent(TripPlanner1_1.this, TripPlanner3.class);

            int currentDay = getIntent().getIntExtra("currentDay", 1);
            Log.d("TripPlanner1_1", "전달하는 currentDay 값: " + currentDay);
            sendIntent.putExtra("currentDay", currentDay);

            sendIntent.putStringArrayListExtra("selectedPlaces", new ArrayList<>(selectedPlaces));
            sendIntent.putStringArrayListExtra("selectedPlaceAddresses", new ArrayList<>(selectedAddresses));
            sendIntent.putStringArrayListExtra("selectedLocations", getIntent().getStringArrayListExtra("selectedLocations"));
            sendIntent.putParcelableArrayListExtra("selectedDates", getIntent().getParcelableArrayListExtra("selectedDates"));
            sendIntent.putExtra("numOfNights", getIntent().getIntExtra("numOfNights", 0));
            sendIntent.putExtra("numOfDays", getIntent().getIntExtra("numOfDays", 1));

            ArrayList<String> latitudeStrings = new ArrayList<>();
            ArrayList<String> longitudeStrings = new ArrayList<>();
            for (int i = 0; i < placeLatitudes.size(); i++) {
                latitudeStrings.add(String.valueOf(placeLatitudes.get(i)));
                longitudeStrings.add(String.valueOf(placeLongitudes.get(i)));
            }
            sendIntent.putStringArrayListExtra("placeLatitudes", latitudeStrings);
            sendIntent.putStringArrayListExtra("placeLongitudes", longitudeStrings);

            ArrayList<String> distances = new ArrayList<>();
            for (int i = 0; i < selectedPlaces.size() - 1; i++) {
                String distanceKey = i + "_" + (i + 1);
                DistanceInfo distanceInfo = distanceMap.get(distanceKey);
                if (distanceInfo != null) {
                    distances.add(distanceInfo.distance);
                }
            }
            sendIntent.putStringArrayListExtra("distances", distances);

            Log.d("DataTransfer", "전달되는 장소 데이터:");
            for (int i = 0; i < selectedPlaces.size(); i++) {
                Log.d("DataTransfer", String.format(
                        "%d번째 장소: %s, 주소: %s, 위도: %s, 경도: %s",
                        i + 1,
                        selectedPlaces.get(i),
                        selectedAddresses.get(i),
                        latitudeStrings.get(i),
                        longitudeStrings.get(i)
                ));
            }
            Log.d("DataTransfer", "전달되는 거리 데이터: " + distances);

            startActivity(sendIntent);
        });

    }

    // 생명주기 메서드_2 :: 액티비티 재개시 TopSheet를 최상단으로 가져오는 메서드
    @Override
    protected void onResume() {
        super.onResume();
        topSheet.bringToFront();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setupSheet() {
        topSheet.post(() -> {
            // 체크박스 영역은 항상 표시되도록 설정
            findViewById(R.id.tag_scroll).setVisibility(View.VISIBLE);

            // 초기 위치를 350dp 위로 설정
            float initialY = convertDpToPx(410);
            topSheet.setTranslationY(-initialY);

            // 최대 확장 높이 계산 (0dp까지)
            expandedY = 0;  // 완전히 내려왔을 때 0dp
        });

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

                        // 이동 범위 제한 (-350dp ~ 0dp)
                        newTranslationY = Math.max(-convertDpToPx(410), Math.min(newTranslationY, 0));
                        topSheet.setTranslationY(newTranslationY);

                        // `tag_scroll` 항상 표시
                        findViewById(R.id.tag_scroll).setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float translationY = topSheet.getTranslationY();
                        if (Math.abs(translationY) < convertDpToPx(175)) { // 중간 지점을 기준으로
                            expandSheet();
                        } else {
                            collapseSheet();
                        }
                        return true;
                }
                return false;
            }
        });

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
                .translationY(0) // 완전히 내려온 상태 (0dp)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void collapseSheet() {
        isExpanded = false;
        topSheet.animate()
                .translationY(-convertDpToPx(410)) // 위로 350dp 올라간 상태
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////

    //태그 필터링 메서드
    private void filterPlacesByTags() {
        SharedPreferences sharedPreferences = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        String placeNamesJson = sharedPreferences.getString("place_names", null);
        String placeAddressesJson = sharedPreferences.getString("place_addresses", null);
        String latitudesJson = sharedPreferences.getString("latitudes", null);
        String longitudesJson = sharedPreferences.getString("longitudes", null);
        String placeTypeJson = sharedPreferences.getString("place_type", null);
        String placeTagsJson = sharedPreferences.getString("place_tags", null);

        if (placeNamesJson == null || placeTagsJson == null) {
            Toast.makeText(this, "장소 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        ArrayList<String> placeNames = gson.fromJson(placeNamesJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<String> placeAddresses = gson.fromJson(placeAddressesJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<Double> latitudes = gson.fromJson(latitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
        ArrayList<Double> longitudes = gson.fromJson(longitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
        ArrayList<String> placeTypes = gson.fromJson(placeTypeJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<List<String>> placeTags = gson.fromJson(placeTagsJson, new TypeToken<ArrayList<List<String>>>() {}.getType());

        ArrayList<Integer> filteredIndices = new ArrayList<>();

        if (selectedTagFilters.isEmpty()) {
            for (int i = 0; i < placeNames.size(); i++) {
                filteredIndices.add(i); // 모든 장소 선택
            }
        } else {
            for (int i = 0; i < placeNames.size(); i++) {
                List<String> tags = placeTags.get(i);
                if (tags != null && !Collections.disjoint(tags, selectedTagFilters)) {
                    filteredIndices.add(i);
                }
            }
        }

        // 필터링된 데이터와 함께 모든 필수 데이터 전달
        ArrayList<String> filteredNames = new ArrayList<>();
        ArrayList<String> filteredAddresses = new ArrayList<>();
        ArrayList<Double> filteredLatitudes = new ArrayList<>();
        ArrayList<Double> filteredLongitudes = new ArrayList<>();
        ArrayList<String> filteredTypes = new ArrayList<>();
        ArrayList<List<String>> filteredTags = new ArrayList<>();

        for (int index : filteredIndices) {
            filteredNames.add(placeNames.get(index));
            filteredAddresses.add(placeAddresses.get(index));
            filteredLatitudes.add(latitudes.get(index));
            filteredLongitudes.add(longitudes.get(index));
            filteredTypes.add(placeTypes.get(index));
            filteredTags.add(placeTags.get(index));
        }

        updatePlaceList(filteredNames, filteredAddresses, filteredLatitudes, filteredLongitudes, filteredTypes, filteredTags);
    }

    private void updatePlaceList(
            ArrayList<String> placeNames,
            ArrayList<String> placeAddresses,
            ArrayList<Double> latitudes,
            ArrayList<Double> longitudes,
            ArrayList<String> placeTypes,
            ArrayList<List<String>> placeTags
    ) {
        if (placeNames.isEmpty() || placeAddresses.isEmpty() || placeTypes.isEmpty()) {
            Toast.makeText(this, "표시할 장소가 없습니다.", Toast.LENGTH_SHORT).show();
            selectedPlaceList.removeAllViews();
            return;
        }

        selectedPlaceList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < placeNames.size(); i++) {
            View itemView = inflater.inflate(R.layout.inc_location_item_1_1, selectedPlaceList, false);
            TextView placeNameView = itemView.findViewById(R.id.place_name);
            TextView placeAddressView = itemView.findViewById(R.id.place_addresses);
            TextView placeTypeView = itemView.findViewById(R.id.place_type_name);
            ImageView placeTypeIcon = itemView.findViewById(R.id.place_type_icon);
            ImageView selectionIcon = itemView.findViewById(R.id.selection_icon_1_1);

            String placeName = placeNames.get(i);
            String placeAddress = placeAddresses.get(i);
            String placeType = placeTypes.get(i);
            Double latitude = latitudes.isEmpty() ? null : latitudes.get(i);
            Double longitude = longitudes.isEmpty() ? null : longitudes.get(i);
            List<String> tags = placeTags.isEmpty() ? new ArrayList<>() : placeTags.get(i);

            placeNameView.setText(truncateText(placeName, 20));
            placeAddressView.setText(truncateText(placeAddress, 30));
            placeTypeView.setVisibility(View.GONE); // 텍스트뷰 숨기기

            // placeType에 따라 이미지 설정
            switch (placeType) {
                case "레스토랑":
                    placeTypeIcon.setImageResource(R.drawable.ic_restaurant);
                    break;
                case "카페/디저트":
                    placeTypeIcon.setImageResource(R.drawable.ic_cafe);
                    break;
                case "관광명소":
                    placeTypeIcon.setImageResource(R.drawable.ic_tourist);
                    break;
                case "자연경관":
                    placeTypeIcon.setImageResource(R.drawable.ic_nature);
                    break;
                case "공원/정원":
                    placeTypeIcon.setImageResource(R.drawable.ic_park);
                    break;
                case "박물관/전시관":
                    placeTypeIcon.setImageResource(R.drawable.ic_museum);
                    break;
                case "체험/테마파크":
                    placeTypeIcon.setImageResource(R.drawable.ic_themepark);
                    break;
                case "한식/일식":
                    placeTypeIcon.setImageResource(R.drawable.ic_food);
                    break;
                case "해변/바다":
                    placeTypeIcon.setImageResource(R.drawable.ic_beach);
                    break;
                case "산/오름":
                    placeTypeIcon.setImageResource(R.drawable.ic_mountain);
                    break;
                case "산/트래킹":
                    placeTypeIcon.setImageResource(R.drawable.ic_hiking);
                    break;
                case "역사유적":
                    placeTypeIcon.setImageResource(R.drawable.ic_historical);
                    break;
                case "랜드마크":
                    placeTypeIcon.setImageResource(R.drawable.ic_landmark);
                    break;
                case "자연동굴/지형":
                    placeTypeIcon.setImageResource(R.drawable.ic_cave);
                    break;
                default:
                    placeTypeIcon.setImageResource(R.drawable.ic_default);
                    break;
            }

            itemView.setOnClickListener(v -> {
                handlePlaceSelection(placeName, placeAddress, placeType, latitude, longitude, tags, selectionIcon);
            });

            selectionIcon.setImageResource(selectedPlaces.contains(placeName) ?
                    R.drawable.ic_checked_circle : R.drawable.drawable_circle_unfilled);

            selectedPlaceList.addView(itemView);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    //검색 필터링 메서드
    private void filterPlacesBySearchText(String searchText) {
        SharedPreferences sharedPreferences = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        Gson gson = new Gson();

        // 저장된 장소 데이터 로드
        String placeNamesJson = sharedPreferences.getString("place_names", null);
        String placeAddressesJson = sharedPreferences.getString("place_addresses", null);
        String latitudesJson = sharedPreferences.getString("latitudes", null);
        String longitudesJson = sharedPreferences.getString("longitudes", null);
        String placeTypesJson = sharedPreferences.getString("place_type", null);
        String placeTagsJson = sharedPreferences.getString("place_tags", null);

        if (placeNamesJson == null || placeAddressesJson == null || latitudesJson == null || longitudesJson == null) {
            Toast.makeText(this, "장소 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> placeNames = gson.fromJson(placeNamesJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<String> placeAddresses = gson.fromJson(placeAddressesJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<Double> latitudes = gson.fromJson(latitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
        ArrayList<Double> longitudes = gson.fromJson(longitudesJson, new TypeToken<ArrayList<Double>>() {}.getType());
        ArrayList<String> placeTypes = gson.fromJson(placeTypesJson, new TypeToken<ArrayList<String>>() {}.getType());
        ArrayList<List<String>> placeTags = gson.fromJson(placeTagsJson, new TypeToken<ArrayList<List<String>>>() {}.getType());

        // 검색어가 포함된 항목 필터링
        ArrayList<String> filteredNames = new ArrayList<>();
        ArrayList<String> filteredAddresses = new ArrayList<>();
        ArrayList<Double> filteredLatitudes = new ArrayList<>();
        ArrayList<Double> filteredLongitudes = new ArrayList<>();
        ArrayList<String> filteredTypes = new ArrayList<>();
        ArrayList<List<String>> filteredTags = new ArrayList<>();

        for (int i = 0; i < placeNames.size(); i++) {
            // 검색어가 장소 이름, 주소 또는 태그에 포함되는지 확인
            boolean matchesSearch = placeNames.get(i).toLowerCase().contains(searchText.toLowerCase()) ||
                    placeAddresses.get(i).toLowerCase().contains(searchText.toLowerCase()) ||
                    (placeTags.get(i) != null && placeTags.get(i).toString().toLowerCase().contains(searchText.toLowerCase()));

            if (matchesSearch) {
                filteredNames.add(placeNames.get(i));
                filteredAddresses.add(placeAddresses.get(i));
                filteredLatitudes.add(latitudes.get(i));
                filteredLongitudes.add(longitudes.get(i));
                filteredTypes.add(placeTypes.get(i));
                filteredTags.add(placeTags.get(i));
            }
        }

        // UI 업데이트
        updatePlaceList(filteredNames, filteredAddresses, filteredLatitudes, filteredLongitudes, filteredTypes, filteredTags);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////



    // UI 초기화 및 설정_2 :: 동적으로 태그(체크박스)를 생성하는 메서드
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

            // 체크박스 이벤트 처리
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedTagFilters.contains(tag)) {
                        selectedTagFilters.add(tag); // 태그 추가
                    }
                } else {
                    selectedTagFilters.remove(tag); // 태그 제거
                }
                filterPlacesByTags(); // 리스트 필터링
            });

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

    // UI 초기화 및 설정_3 ::  동적으로 장소 리스트를 생성하는 메서드
    private void addDynamicPlaces() {
        SharedPreferences sharedPreferences = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        Gson gson = new Gson();

        // SharedPreferences에서 데이터 읽기
        String placeNamesJson = sharedPreferences.getString("place_names", null);
        String placeAddressesJson = sharedPreferences.getString("place_addresses", null);
        String latitudesJson = sharedPreferences.getString("latitudes", null);
        String longitudesJson = sharedPreferences.getString("longitudes", null);
        String placeTypeJson = sharedPreferences.getString("place_type", null);
        String placeTagsJson = sharedPreferences.getString("place_tags", null);

        if (placeNamesJson != null && placeAddressesJson != null) {
            // JSON 데이터를 ArrayList로 변환
            ArrayList<String> placeNames = gson.fromJson(placeNamesJson, new TypeToken<ArrayList<String>>(){}.getType());
            ArrayList<String> placeAddresses = gson.fromJson(placeAddressesJson, new TypeToken<ArrayList<String>>(){}.getType());
            ArrayList<Double> latitudes = gson.fromJson(latitudesJson, new TypeToken<ArrayList<Double>>(){}.getType());
            ArrayList<Double> longitudes = gson.fromJson(longitudesJson, new TypeToken<ArrayList<Double>>(){}.getType());
            ArrayList<String> placeTypes = gson.fromJson(placeTypeJson, new TypeToken<ArrayList<String>>(){}.getType());
            ArrayList<List<String>> placeTags = gson.fromJson(placeTagsJson, new TypeToken<ArrayList<List<String>>>(){}.getType());

            // 기존 updatePlaceList 메서드 호출하여 UI 업데이트
            updatePlaceList(placeNames, placeAddresses, latitudes, longitudes, placeTypes, placeTags);

            // 데이터 로딩 로그
            for (int i = 0; i < placeNames.size(); i++) {
                Log.d("InitialData", String.format(
                        "장소: %s, 주소: %s, 위도: %f, 경도: %f, 유형: %s, 태그: %s",
                        placeNames.get(i),
                        placeAddresses.get(i),
                        latitudes.get(i),
                        longitudes.get(i),
                        placeTypes.get(i),
                        placeTags.get(i).toString()
                ));
            }
        }
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 장소 선택 및 관리 로직_0 :: 거리 정보 관리를 위한 Data Class
    private class DistanceInfo {
        String distance;
        boolean isCalculating;

        DistanceInfo(String distance, boolean isCalculating) {
            this.distance = distance;
            this.isCalculating = isCalculating;
        }
    }

    // 장소 선택 및 관리 로직_0 :: 거리 정보를 저장할 Map
    private Map<String, DistanceInfo> distanceMap = new HashMap<>();

    // 장소 선택 및 관리 로직_1 :: 장소 선택 처리 메서드
    private void handlePlaceSelection(String placeName, String placeAddress, String placeType,
                                      Double latitude, Double longitude, List<String> tags,
                                      ImageView selectionIcon) {
        if (!selectedPlaces.contains(placeName)) {
            selectedPlaces.add(placeName);
            selectedAddresses.add(placeAddress);
            selectedType.add(placeType);
            placeLatitudes.add(latitude);
            placeLongitudes.add(longitude);
            selectionIcon.setImageResource(R.drawable.ic_checked_circle);

            Log.d("PlaceSelection", String.format(
                    "장소 선택됨 -> 이름: %s, 주소: %s, 위도: %f, 경도: %f",
                    placeName, placeAddress, latitude, longitude
            ));

            int newIndex = selectedPlaces.size() - 1;
            if (newIndex > 0) {
                String newDistanceKey = (newIndex - 1) + "_" + newIndex;
                distanceMap.remove(newDistanceKey);
            }
        } else {
            int index = selectedPlaces.indexOf(placeName);
            Log.d("PlaceSelection", String.format(
                    "장소 선택 해제됨 -> 이름: %s, 주소: %s",
                    placeName, selectedAddresses.get(index)
            ));

            if (index > 0) {
                distanceMap.remove((index - 1) + "_" + index);
            }
            if (index < selectedPlaces.size() - 1) {
                distanceMap.remove(index + "_" + (index + 1));
            }
            if (index > 0 && index < selectedPlaces.size() - 1) {
                distanceMap.remove((index - 1) + "_" + (index + 1));
            }

            removePlaceFromContainer(index);
            selectionIcon.setImageResource(R.drawable.drawable_circle_unfilled);
        }
        updateSelectedPlaces();
    }

    // 장소 선택 및 관리 로직_2 ::  선택된 장소들을 업데이트하고 표시하는 메서드
    private void updateSelectedPlaces() {
        selectedPlacesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < selectedPlaces.size(); i++) {
            // 장소 아이템 추가
            View placeView = inflater.inflate(R.layout.inc_selected_region_item_1_1, selectedPlacesContainer, false);

            FrameLayout placeImage = placeView.findViewById(R.id.region_image);
            ImageView innerImage = placeImage.findViewById(R.id.inner_image);
            TextView placeNameView = placeView.findViewById(R.id.region_name);
            ImageView removeIcon = placeView.findViewById(R.id.remove_icon);

            // 장소명 설정
            placeNameView.setText(truncateText(selectedPlaces.get(i), 8));

            // 원형 이미지 설정
            innerImage.setImageResource(R.drawable.drawable_gray_circle);

            int finalI = i;
            removeIcon.setOnClickListener(v -> {
                removePlaceFromContainer(finalI);
                refreshPlaceList();
            });

            selectedPlacesContainer.addView(placeView);

            // 마지막 아이템이 아닌 경우 화살표와 거리 표시 추가
            if (i < selectedPlaces.size() - 1) {
                LinearLayout arrowLayout = new LinearLayout(this);
                arrowLayout.setOrientation(LinearLayout.VERTICAL);
                arrowLayout.setGravity(Gravity.CENTER);

                // 거리 표시 컨테이너
                FrameLayout distanceContainer = new FrameLayout(this);

                // 거리 텍스트뷰
                TextView distanceTextView = new TextView(this);
                distanceTextView.setGravity(Gravity.CENTER);
                distanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                distanceTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                distanceTextView.setTag("distance_text");

                // 프로그레스바
                ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
                progressBar.setTag("progress_bar");
                progressBar.setIndeterminate(true);

                FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );

                distanceContainer.addView(progressBar, centerParams);
                distanceContainer.addView(distanceTextView, centerParams);

                // 화살표 이미지
                ImageView arrowIcon = new ImageView(this);
                arrowIcon.setImageResource(R.drawable.ic_double_arrow);

                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                containerParams.setMargins(0, convertDpToPx(4), 0, convertDpToPx(4));

                LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                        convertDpToPx(16),
                        convertDpToPx(16)
                );

                arrowLayout.addView(distanceContainer, containerParams);
                arrowLayout.addView(arrowIcon, arrowParams);

                // 마진 설정
                LinearLayout.LayoutParams arrowLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                arrowLayoutParams.setMargins(convertDpToPx(4), 0, convertDpToPx(4), 0);

                selectedPlacesContainer.addView(arrowLayout, arrowLayoutParams);

                // 거리 정보 확인 및 표시
                String distanceKey = i + "_" + (i + 1);
                DistanceInfo savedDistance = distanceMap.get(distanceKey);

                if (savedDistance != null && !savedDistance.isCalculating) {
                    // 이미 계산된 거리가 있는 경우
                    distanceTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    distanceTextView.setText(savedDistance.distance);
                } else {
                    // 새로운 거리 계산이 필요한 경우
                    distanceTextView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    distanceMap.put(distanceKey, new DistanceInfo("계산 중...", true));
                    calculateDistance(i, i + 1, arrowLayout);
                }
            }
        }

        updateButtonText();
    }

    // 장소 선택 및 관리 로직_3 ::  장소 제거 메서드
    private void removePlaceFromContainer(int index) {
        if (index < 0 || index >= selectedPlaces.size()) return;

        if (index > 0) {
            distanceMap.remove((index - 1) + "_" + index);
        }
        if (index < selectedPlaces.size() - 1) {
            distanceMap.remove(index + "_" + (index + 1));
        }
        if (index > 0 && index < selectedPlaces.size() - 1) {
            for (int i = index - 1; i <= index + 1; i++) {
                for (int j = i + 1; j <= index + 1; j++) {
                    distanceMap.remove(i + "_" + j);
                }
            }
        }

        selectedPlaces.remove(index);
        selectedAddresses.remove(index);
        placeLatitudes.remove(index);
        placeLongitudes.remove(index);

        updateSelectedPlaces();
    }

    // 장소 선택 및 관리 핵심 로직_4 ::  장소 리스트를 새로고침하는 메서드
    private void refreshPlaceList() {
        selectedPlaceList.removeAllViews();
        addDynamicPlaces();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 거리 계산 관련_1 :: 두 장소 사이의 거리를 계산하고 UI에 표시하는 메서드
    private void calculateDistance(int startIdx, int endIdx, LinearLayout arrowLayout) {
        if (startIdx >= placeLatitudes.size() || endIdx >= placeLongitudes.size()) {
            return;
        }

        String distanceKey = startIdx + "_" + endIdx;
        String requestTag = "distance_" + distanceKey;
        arrowLayout.setTag(R.id.distance_request_tag, requestTag);

        Double startLatitude = placeLatitudes.get(startIdx);
        Double startLongitude = placeLongitudes.get(startIdx);
        Double endLatitude = placeLatitudes.get(endIdx);
        Double endLongitude = placeLongitudes.get(endIdx);

        String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start="
                + startLongitude + "," + startLatitude + "&goal=" + endLongitude + "," + endLatitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", apiKeyId)
                .addHeader("X-NCP-APIGW-API-KEY", apiKey)
                .tag(requestTag)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    distanceMap.put(distanceKey, new DistanceInfo("계산 중...", true));
                    updateDistanceText(arrowLayout, "계산 중...");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        distanceMap.put(distanceKey, new DistanceInfo("재시도 중...", true));
                        updateDistanceText(arrowLayout, "재시도 중...");
                    });
                    return;
                }

                String responseData = response.body().string();
                try {
                    runOnUiThread(() -> {
                        String currentTag = (String) arrowLayout.getTag(R.id.distance_request_tag);
                        if (currentTag != null && currentTag.equals(requestTag)) {
                            try {
                                JSONObject jsonObject = new JSONObject(responseData);
                                JSONObject route = jsonObject.getJSONObject("route")
                                        .getJSONArray("traoptimal")
                                        .getJSONObject(0);
                                double distance = route.getJSONObject("summary").getInt("distance") / 1000.0;
                                String distanceText = String.format("%.1f km", distance);

                                // 계산된 거리를 Map에 저장
                                distanceMap.put(distanceKey, new DistanceInfo(distanceText, false));
                                updateDistanceText(arrowLayout, distanceText);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                distanceMap.put(distanceKey, new DistanceInfo("거리 계산 실패", false));
                                updateDistanceText(arrowLayout, "거리 계산 실패");
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 거리 계산 관련_2 :: 거리 계산 상태에 따라 텍스트와 프로그레스바 표시를 전환
    private void updateDistanceText(LinearLayout arrowLayout, String text) {
        TextView distanceText = arrowLayout.findViewWithTag("distance_text");
        ProgressBar progressBar = arrowLayout.findViewWithTag("progress_bar");

        if (distanceText != null && progressBar != null) {
            if (text.contains("계산") || text.contains("재시도")) {
                distanceText.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                distanceText.setVisibility(View.VISIBLE);
                distanceText.setText(text);
                distanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // UI 유틸리티 메서드_1 :: DP를 픽셀 단위로 변환
    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // UI 유틸리티 메서드_2 :: 텍스트 최대길이 초과 시 '...' 표시
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // UI 유틸리티 메서드_3 :: 버튼 텍스트 업데이트
    private void updateButtonText() {
        TextView btnSelected = findViewById(R.id.btn_selected);
        if (selectedPlaces.size() > 1) {
            String firstPlace = truncateText(selectedPlaces.get(0), 8);
            btnSelected.setText(firstPlace + " 외 " + (selectedPlaces.size() - 1) + "개 선택 완료");
        } else if (selectedPlaces.size() == 1) {
            String place = truncateText(selectedPlaces.get(0), 12);
            btnSelected.setText(place + " 선택 완료");
        } else {
            btnSelected.setText("가고싶은곳이 있나요?");
        }
    }

}
