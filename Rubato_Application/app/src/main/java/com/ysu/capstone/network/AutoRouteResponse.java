package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class AutoRouteResponse {
    @SerializedName("response")
    private String response;

    @SerializedName("schedule")
    private Map<String, List<Place>> schedule;

    public static class Place {
        @SerializedName("name")
        private String name;

        @SerializedName("address")
        private String address;

        @SerializedName("latitude")
        private String latitude;

        @SerializedName("longitude")
        private String longitude;

        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getLatitude() { return latitude; }
        public String getLongitude() { return longitude; }
    }

    public String getStatus() {
        return "completed";  // 성공적으로 응답을 받았다면 completed 반환
    }

    public String getResponse() { return response; }
    public Map<String, List<Place>> getSchedule() { return schedule; }
}