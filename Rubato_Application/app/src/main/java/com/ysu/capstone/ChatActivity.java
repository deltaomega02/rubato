package com.ysu.capstone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ysu.capstone.chat.Message;
import com.ysu.capstone.chat.MessageAdapter;
import com.ysu.capstone.decorators.DotLoadingAnimation;
import com.ysu.capstone.network.ApiService;
import com.ysu.capstone.network.ChatRequest;
import com.ysu.capstone.network.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText etMsg;
    private ImageButton btnSend;
    private boolean isRequestInProgress = false;
    private String userName;
    private boolean shouldAutoScroll = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ArrayList<String> selectedPlaces = new ArrayList<>();
    private ArrayList<String> selectedAddresses = new ArrayList<>();
    private ArrayList<String> selectedLatitudes = new ArrayList<>();
    private ArrayList<String> selectedLongitudes = new ArrayList<>();

    // 여행 관련 데이터
    private ArrayList<String> selectedLocations;
    private ArrayList<String> selectedDates;
    private int numOfDays;
    private int currentDay;
    private HashMap<Integer, List<String>> dayWiseDestinations = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);


        // 기본 UI 초기화
        initializeUI();

        // TripPlanner3에서 전달받은 데이터 로드
        loadTripData();

        btnSend.setOnClickListener(v -> {
            String message = etMsg.getText().toString();
            if (!message.isEmpty() && !isRequestInProgress) {
                sendMessage(message);
                etMsg.setText("");
            }
        });
    }

    private void initializeUI() {
        recyclerView = findViewById(R.id.recyclerView);
        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);
        ImageView backButton = findViewById(R.id.back_button);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        userName = sharedPreferences.getString("user_name", "default_user");

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        setupScrollListener();
        loadChatHistory();

        backButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                View viewToBlur = findViewById(R.id.full_screen);
                if (viewToBlur != null) {
                    viewToBlur.setRenderEffect(null);
                }
            }
            onBackPressed();
        });
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    shouldAutoScroll = false;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
                    if (lastVisiblePosition == messageAdapter.getItemCount() - 1) {
                        shouldAutoScroll = true;
                    }
                }
            }
        });
    }

    private void loadTripData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedPlaces = intent.getStringArrayListExtra("selectedPlaces");
            selectedAddresses = intent.getStringArrayListExtra("selectedPlaceAddresses");
            selectedLatitudes = intent.getStringArrayListExtra("placeLatitudes");
            selectedLongitudes = intent.getStringArrayListExtra("placeLongitudes");
            selectedLocations = intent.getStringArrayListExtra("selectedLocations");
            selectedDates = intent.getStringArrayListExtra("selectedDates");
            numOfDays = intent.getIntExtra("numOfDays", 1);
            currentDay = intent.getIntExtra("currentDay", 1);

            Log.d("ChatActivity", "Loaded trip data:");
            Log.d("ChatActivity", "Selected Places: " + selectedPlaces);
            Log.d("ChatActivity", "Selected Locations: " + selectedLocations);
            Log.d("ChatActivity", "Number of Days: " + numOfDays);
            Log.d("ChatActivity", "Current Day: " + currentDay);
        }
    }

    private void sendMessage(String message) {
        messageList.add(new Message(message, "user"));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        final Message loadingMessage = new Message("loading", "bot");
        messageList.add(loadingMessage);
        final int loadingMessageIndex = messageList.size() - 1;
        messageAdapter.notifyItemInserted(loadingMessageIndex);

        startDotAnimationForLoadingMessage(loadingMessageIndex);
        btnSend.setImageResource(R.drawable.ic_send_off);

        // SharedPreferences에서 사용자 정보 가져오기
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userEmail = userPrefs.getString("user_email", "");
        String sessionId = userPrefs.getString("chat_session_id", "");

        // API 요청 데이터 준비
        JSONObject requestData = new JSONObject();
        try {
            // 채팅 데이터
            requestData.put("prompt", message);
            requestData.put("selectedDates", new JSONArray(selectedDates));
            requestData.put("selectedLocations", new JSONArray(selectedLocations));
            requestData.put("numOfDays", numOfDays);
            requestData.put("currentDay", currentDay);

            // 현재 선택된 장소들
            requestData.put("placeNames", new JSONArray(selectedPlaces != null ? selectedPlaces : new ArrayList<>()));
            requestData.put("placeAddresses", new JSONArray(selectedAddresses != null ? selectedAddresses : new ArrayList<>()));
            requestData.put("latitudes", new JSONArray(selectedLatitudes != null ? selectedLatitudes : new ArrayList<>()));
            requestData.put("longitudes", new JSONArray(selectedLongitudes != null ? selectedLongitudes : new ArrayList<>()));

            // SharedPreferences에서 전체 장소 목록 가져오기
            SharedPreferences tripPrefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
            Gson gson = new Gson();
            ArrayList<String> allPlaceNames = gson.fromJson(tripPrefs.getString("place_names", "[]"),
                    new TypeToken<ArrayList<String>>(){}.getType());
            ArrayList<String> allAddresses = gson.fromJson(tripPrefs.getString("place_addresses", "[]"),
                    new TypeToken<ArrayList<String>>(){}.getType());
            ArrayList<Double> allLatitudes = gson.fromJson(tripPrefs.getString("latitudes", "[]"),
                    new TypeToken<ArrayList<Double>>(){}.getType());
            ArrayList<Double> allLongitudes = gson.fromJson(tripPrefs.getString("longitudes", "[]"),
                    new TypeToken<ArrayList<Double>>(){}.getType());

            // 전체 장소 목록 추가
            requestData.put("allPlaceNames", new JSONArray(allPlaceNames));
            requestData.put("allPlaceAddresses", new JSONArray(allAddresses));
            requestData.put("allLatitudes", new JSONArray(allLatitudes));
            requestData.put("allLongitudes", new JSONArray(allLongitudes));

        } catch (JSONException e) {
            e.printStackTrace();
            showError("데이터 준비 중 오류가 발생했습니다");
            return;
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        ChatRequest chatRequest = new ChatRequest(requestData.toString(), userName, userEmail, sessionId);

        isRequestInProgress = true;
        btnSend.setEnabled(false);

        apiService.sendChatRequest(chatRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processStreamResponse(response.body(), loadingMessageIndex);
                } else {
                    runOnUiThread(() -> showError("서버 응답 오류가 발생했습니다"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                runOnUiThread(() -> showError("네트워크 오류: " + t.getMessage()));
            }
        });
    }

    private void processStreamResponse(ResponseBody responseBody, int messageIndex) {
        new Thread(() -> {
            StringBuilder accumulatedResponse = new StringBuilder();
            StringBuilder jsonBuffer = new StringBuilder();
            boolean isJSONStarted = false;
            int braceCount = 0;
            boolean responseReceived = false;
            int chunkCount = 0;

            Log.d("ChatResponse", "Starting to process stream response");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    responseReceived = true;
                    chunkCount++;
                    Log.d("StreamData", String.format("Chunk #%d raw data: %s", chunkCount, line));

                    line = line.trim();
                    if (line.isEmpty()) {
                        Log.d("StreamData", String.format("Chunk #%d: Empty line", chunkCount));
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        Log.d("StreamData", String.format("Chunk #%d: Invalid format - %s", chunkCount, line));
                        continue;
                    }

                    line = line.substring(5).trim();
                    if (line.isEmpty()) {
                        Log.d("StreamData", String.format("Chunk #%d: Empty data content", chunkCount));
                        continue;
                    }
                    if (line.equals("[DONE]")) {
                        Log.d("StreamData", String.format("Chunk #%d: Stream end marker", chunkCount));
                        continue;
                    }

                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        Log.d("StreamJSON", String.format("Chunk #%d parsed JSON: %s",
                                chunkCount, jsonObject.toString(2)));

                        if (!jsonObject.has("choices")) {
                            Log.e("StreamJSON", String.format("Chunk #%d: Missing 'choices' in response: %s",
                                    chunkCount, jsonObject.toString(2)));
                            continue;
                        }

                        JSONArray choices = jsonObject.getJSONArray("choices");
                        if (choices.length() == 0) {
                            Log.e("StreamJSON", String.format("Chunk #%d: Empty choices array", chunkCount));
                            continue;
                        }

                        String content = choices.getJSONObject(0)
                                .getJSONObject("delta")
                                .optString("content", "");

                        Log.d("StreamContent", String.format("Chunk #%d content: %s", chunkCount, content));

                        accumulatedResponse.append(content);
                        Log.d("AccumulatedResponse", String.format("After chunk #%d - Total length: %d\nCurrent full content: %s",
                                chunkCount, accumulatedResponse.length(), accumulatedResponse.toString()));

                        for (char c : content.toCharArray()) {
                            if (c == '{') {
                                if (braceCount == 0) {
                                    isJSONStarted = true;
                                    jsonBuffer.setLength(0);
                                    Log.d("JSONParsing", String.format("Chunk #%d: Started JSON accumulation", chunkCount));
                                }
                                braceCount++;
                            } else if (c == '}') {
                                braceCount--;
                                if (braceCount == 0 && isJSONStarted) {
                                    Log.d("JSONParsing", String.format("Chunk #%d: Completed JSON accumulation", chunkCount));
                                }
                            }
                        }

                        if (isJSONStarted) {
                            jsonBuffer.append(content);
                            if (braceCount == 0) {
                                String jsonStr = jsonBuffer.toString();
                                try {
                                    JSONObject responseJson = new JSONObject(jsonStr);
                                    Log.d("JSONParsing", String.format("Chunk #%d: Valid JSON formed: %s",
                                            chunkCount, responseJson.toString(4)));

                                    if (responseJson.has("schedule")) {
                                        Log.d("ChatResponse", "Schedule JSON detected, creating card");
                                        runOnUiThread(() -> {
                                            stopDotAnimationForLoadingMessage(messageIndex);
                                            messageList.remove(messageIndex);
                                            messageAdapter.notifyItemRemoved(messageIndex);
                                            createScheduleCard(responseJson);
                                        });
                                        return;
                                    }
                                } catch (JSONException e) {
                                    Log.e("JSONParsing", String.format("Chunk #%d: Failed to parse JSON: %s\nError: %s",
                                            chunkCount, jsonStr, e.getMessage()));
                                }
                                isJSONStarted = false;
                            }
                        }

                    } catch (JSONException e) {
                        Log.e("StreamParsing", String.format("Chunk #%d: Parse error: %s\nLine: %s",
                                chunkCount, e.getMessage(), line));
                    }
                }

                if (!responseReceived) {
                    Log.e("ChatResponse", "No data received from server after reading stream");
                    runOnUiThread(() -> showError("서버로부터 데이터를 받지 못했습니다"));
                    return;
                }

                String finalResponse = accumulatedResponse.toString();
                Log.d("FinalResponse", String.format("Processing completed\nTotal chunks: %d\nFinal content length: %d\nComplete content:\n%s",
                        chunkCount, finalResponse.length(), finalResponse));

                if (finalResponse.isEmpty()) {
                    Log.e("ChatResponse", "Empty accumulated response after processing all chunks");
                    runOnUiThread(() -> showError("빈 응답을 받았습니다"));
                    return;
                }

                if (!isJSONStarted) {
                    Log.d("ChatResponse", "Processing text response");
                    runOnUiThread(() -> {
                        stopDotAnimationForLoadingMessage(messageIndex);
                        displayTextCharacterByCharacter(finalResponse, messageIndex);
                    });
                }

            } catch (IOException e) {
                Log.e("ChatResponse", "Stream reading error", e);
                runOnUiThread(() -> showError("응답 읽기 실패: " + e.getMessage()));
            } finally {
                Log.d("ChatResponse", String.format("Stream processing completed\nResponse received: %s\nTotal chunks: %d",
                        responseReceived, chunkCount));
                runOnUiThread(() -> {
                    isRequestInProgress = false;
                    btnSend.setEnabled(true);
                });
            }
        }).start();
    }

    private void createScheduleCard(JSONObject responseJson) {
        try {
            View scheduleCard = LayoutInflater.from(this).inflate(R.layout.schedule_card, null);
            LinearLayout daysContainer = scheduleCard.findViewById(R.id.days_container);

            JSONObject schedule = responseJson.getJSONObject("schedule");
            Iterator<String> keys = schedule.keys();

            // 날짜 정렬을 위해 TreeMap 사용
            Map<String, JSONArray> sortedSchedule = new TreeMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                sortedSchedule.put(key, schedule.getJSONArray(key));
            }

            // HashMap을 final로 선언
            final HashMap<Integer, List<String>> newDestinations = new HashMap<>();
            int dayCounter = 1;

            for (Map.Entry<String, JSONArray> entry : sortedSchedule.entrySet()) {
                String key = entry.getKey(); // 날짜 키 (예: "2024-11-19")
                JSONArray placesArray = entry.getValue();

                View dayView = LayoutInflater.from(this).inflate(R.layout.day_schedule_item, null);
                TextView dayTitle = dayView.findViewById(R.id.day_title);
                TextView placesList = dayView.findViewById(R.id.places_list);

                dayTitle.setText(dayCounter + "일차");
                StringBuilder placesText = new StringBuilder();
                List<String> dayPlaces = new ArrayList<>();

                for (int i = 0; i < placesArray.length(); i++) {
                    JSONObject placeObject = placesArray.getJSONObject(i);
                    String placeName = placeObject.getString("name");
                    String address = placeObject.getString("address");
                    String latitude = placeObject.getString("latitude");
                    String longitude = placeObject.getString("longitude");

                    String placeInfo = String.format("%s,%s,%s,%s",
                            placeName, address, latitude, longitude);
                    dayPlaces.add(placeInfo);
                    placesText.append("• ").append(placeName).append("\n");
                }

                placesList.setText(placesText.toString());
                newDestinations.put(dayCounter, dayPlaces);
                daysContainer.addView(dayView);

                dayCounter++; // 다음 날로 이동
            }

            Button addScheduleButton = scheduleCard.findViewById(R.id.add_schedule_button);
            addScheduleButton.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                // schedule JSON 전체를 전달
                resultIntent.putExtra("schedule", responseJson.toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            });

            Message cardMessage = new Message("schedule_card", "bot");
            cardMessage.setScheduleView(scheduleCard);
            messageList.add(cardMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);

        } catch (JSONException e) {
            e.printStackTrace();
            showError("일정 생성 중 오류가 발생했습니다.");
        }
    }


//    private void createScheduleCard(JSONObject responseJson) {
//        try {
//            View scheduleCard = LayoutInflater.from(this).inflate(R.layout.schedule_card, null);
//            LinearLayout daysContainer = scheduleCard.findViewById(R.id.days_container);
//
//            JSONObject schedule = responseJson.getJSONObject("schedule");
//            Iterator<String> keys = schedule.keys();
//
//            // HashMap을 final로 선언
//            final HashMap<Integer, List<String>> newDestinations = new HashMap<>();
//
//            while (keys.hasNext()) {
//                String key = keys.next();
//                int day = Integer.parseInt(key);
//                JSONArray placesArray = schedule.getJSONArray(key);
//
//                View dayView = LayoutInflater.from(this).inflate(R.layout.day_schedule_item, null);
//                TextView dayTitle = dayView.findViewById(R.id.day_title);
//                TextView placesList = dayView.findViewById(R.id.places_list);
//
//                dayTitle.setText(day + "일차");
//                StringBuilder placesText = new StringBuilder();
//                List<String> dayPlaces = new ArrayList<>();
//
//                for (int i = 0; i < placesArray.length(); i++) {
//                    JSONObject placeObject = placesArray.getJSONObject(i);
//                    String placeName = placeObject.getString("name");
//                    String address = placeObject.getString("address");
//                    String latitude = placeObject.getString("latitude");
//                    String longitude = placeObject.getString("longitude");
//
//                    String placeInfo = String.format("%s,%s,%s,%s",
//                            placeName, address, latitude, longitude);
//                    dayPlaces.add(placeInfo);
//                    placesText.append("• ").append(placeName).append("\n");
//                }
//
//                placesList.setText(placesText.toString());
//                newDestinations.put(day, dayPlaces);
//                daysContainer.addView(dayView);
//            }
//
//            Button addScheduleButton = scheduleCard.findViewById(R.id.add_schedule_button);
//            addScheduleButton.setOnClickListener(v -> {
//                Intent resultIntent = new Intent();
//                // schedule JSON 전체를 전달
//                resultIntent.putExtra("schedule", responseJson.toString());
//                setResult(RESULT_OK, resultIntent);
//                finish();
//            });
//
//            Message cardMessage = new Message("schedule_card", "bot");
//            cardMessage.setScheduleView(scheduleCard);
//            messageList.add(cardMessage);
//            messageAdapter.notifyItemInserted(messageList.size() - 1);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//            showError("일정 생성 중 오류가 발생했습니다.");
//        }
//    }

    private void startDotAnimationForLoadingMessage(int messageIndex) {
        recyclerView.post(() -> {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(messageIndex);
            if (viewHolder != null) {
                View dot1 = viewHolder.itemView.findViewById(R.id.dot1);
                View dot2 = viewHolder.itemView.findViewById(R.id.dot2);
                View dot3 = viewHolder.itemView.findViewById(R.id.dot3);
                LinearLayout dotLoadingLayout = viewHolder.itemView.findViewById(R.id.dotLoadingLayout);
                TextView leftChatTv = viewHolder.itemView.findViewById(R.id.leftChatTv);

                if (dot1 != null && dot2 != null && dot3 != null && dotLoadingLayout != null) {
                    DotLoadingAnimation.start(dot1, dot2, dot3);
                    dotLoadingLayout.setVisibility(View.VISIBLE);
                    leftChatTv.setVisibility(View.GONE);
                }
            }
        });
    }

    private void stopDotAnimationForLoadingMessage(int messageIndex) {
        recyclerView.post(() -> {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(messageIndex);
            if (viewHolder != null) {
                View dot1 = viewHolder.itemView.findViewById(R.id.dot1);
                View dot2 = viewHolder.itemView.findViewById(R.id.dot2);
                View dot3 = viewHolder.itemView.findViewById(R.id.dot3);
                LinearLayout dotLoadingLayout = viewHolder.itemView.findViewById(R.id.dotLoadingLayout);
                TextView leftChatTv = viewHolder.itemView.findViewById(R.id.leftChatTv);

                if (dot1 != null && dot2 != null && dot3 != null && dotLoadingLayout != null) {
                    dot1.clearAnimation();
                    dot2.clearAnimation();
                    dot3.clearAnimation();
                    dotLoadingLayout.setVisibility(View.GONE);
                    leftChatTv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void parseAndSaveSchedule(JSONObject schedule) throws JSONException {
        dayWiseDestinations.clear();
        Iterator<String> keys = schedule.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            int day = Integer.parseInt(key);
            JSONArray placesArray = schedule.getJSONArray(key);
            List<String> places = new ArrayList<>();

            for (int i = 0; i < placesArray.length(); i++) {
                places.add(placesArray.getString(i));
            }

            dayWiseDestinations.put(day, places);
        }

        saveDayWiseDestinations();
    }

    private void saveDayWiseDestinations() {
        SharedPreferences prefs = getSharedPreferences("TripPlannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        editor.putString("day_wise_destinations", gson.toJson(dayWiseDestinations));
        editor.apply();

        Log.d("ChatActivity", "Saved schedule: " + dayWiseDestinations);
    }

    private void saveChatHistory() {
        SharedPreferences sharedPreferences = getSharedPreferences("ChatHistory", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        JSONArray jsonArray = new JSONArray();
        for (Message message : messageList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("message", message.getText());
                jsonObject.put("sender", message.getSender());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString("chat_messages", jsonArray.toString());
        editor.apply();
    }

    private void loadChatHistory() {
        SharedPreferences sharedPreferences = getSharedPreferences("ChatHistory", MODE_PRIVATE);
        String chatHistory = sharedPreferences.getString("chat_messages", null);

        if (chatHistory != null) {
            try {
                JSONArray jsonArray = new JSONArray(chatHistory);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String messageText = jsonObject.getString("message");
                    String sender = jsonObject.getString("sender");
                    messageList.add(new Message(messageText, sender));
                }
                messageAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayTextCharacterByCharacter(String content, int messageIndex) {
        final StringBuilder currentText = new StringBuilder();

        handler.post(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (index < content.length()) {
                    currentText.append(content.charAt(index));

                    Message loadingMessage = messageList.get(messageIndex);
                    loadingMessage.setMessage(currentText.toString());
                    messageAdapter.notifyItemChanged(messageIndex);

                    if (shouldAutoScroll) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    index++;

                    handler.postDelayed(this, 20);
                } else {
                    btnSend.setImageResource(R.drawable.ic_send);
                    saveChatHistory();
                }
            }
        });
    }

    private void showError(String errorMessage) {
        messageList.add(new Message(errorMessage, "bot"));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        isRequestInProgress = false;
        btnSend.setEnabled(true);
        btnSend.setImageResource(R.drawable.ic_send);
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("hasNewSchedule", !dayWiseDestinations.isEmpty());
        resultIntent.putExtra("dayWiseDestinations", new Gson().toJson(dayWiseDestinations));
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    class ScheduleIntegrator {
        private SharedPreferences prefs;
        private Context context;

        public ScheduleIntegrator(Context context) {
            this.context = context;
            this.prefs = context.getSharedPreferences("TripPlannerPrefs", Context.MODE_PRIVATE);
        }

        public void processSchedule(JSONObject scheduleJson,
                                    ArrayList<String> selectedPlaces,
                                    ArrayList<String> selectedAddresses,
                                    ArrayList<String> selectedLatitudes,
                                    ArrayList<String> selectedLongitudes) throws JSONException {
            JSONObject schedule = scheduleJson.getJSONObject("schedule");

            // 전달받은 리스트 초기화
            selectedPlaces.clear();
            selectedAddresses.clear();
            selectedLatitudes.clear();
            selectedLongitudes.clear();

            // 저장된 장소 데이터 로드
            Gson gson = new Gson();
            ArrayList<String> allPlaceNames = gson.fromJson(
                    prefs.getString("place_names", "[]"),
                    new TypeToken<ArrayList<String>>(){}.getType()
            );
            ArrayList<String> allAddresses = gson.fromJson(
                    prefs.getString("place_addresses", "[]"),
                    new TypeToken<ArrayList<String>>(){}.getType()
            );
            ArrayList<Double> allLatitudes = gson.fromJson(
                    prefs.getString("latitudes", "[]"),
                    new TypeToken<ArrayList<Double>>(){}.getType()
            );
            ArrayList<Double> allLongitudes = gson.fromJson(
                    prefs.getString("longitudes", "[]"),
                    new TypeToken<ArrayList<Double>>(){}.getType()
            );

            // 일정의 각 장소 처리
            Iterator<String> keys = schedule.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray places = schedule.getJSONArray(key);

                for (int i = 0; i < places.length(); i++) {
                    String placeName = places.getString(i);
                    int index = allPlaceNames.indexOf(placeName);

                    if (index != -1) {
                        selectedPlaces.add(placeName);
                        selectedAddresses.add(allAddresses.get(index));
                        selectedLatitudes.add(String.valueOf(allLatitudes.get(index)));
                        selectedLongitudes.add(String.valueOf(allLongitudes.get(index)));
                    }
                }
            }
        }
    }

}