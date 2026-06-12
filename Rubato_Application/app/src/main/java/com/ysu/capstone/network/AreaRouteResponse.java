package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AreaRouteResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<RouteData> data;

    public static class RouteData {
        @SerializedName("route_id")
        private int route_id;

        @SerializedName("user_name")
        private String user_name;

        @SerializedName("likes")
        private int likes;

        @SerializedName("final_score")
        private int final_score;

        @SerializedName("theme_name")
        private String theme_name;

        @SerializedName("estimated_cost")
        private float estimated_cost;

        @SerializedName("total_distance")
        private float total_distance;

        @SerializedName("areas")
        private List<String> areas;

        @SerializedName("tags")
        private List<String> tags;

        @SerializedName("route_details")
        private List<RouteDetail> route_details;

        // Getters
        public int getRoute_id() { return route_id; }
        public String getUser_name() { return user_name; }
        public int getLikes() { return likes; }
        public int getFinal_score() { return final_score; }
        public String getTheme_name() { return theme_name; }
        public float getEstimated_cost() { return estimated_cost; }
        public float getTotal_distance() { return total_distance; }
        public List<String> getAreas() { return areas; }
        public List<String> getTags() { return tags; }
        public List<RouteDetail> getRoute_details() { return route_details; }
    }

    public static class RouteDetail {
        @SerializedName("place_name")
        private String place_name;

        @SerializedName("latitude")
        private float latitude;

        @SerializedName("longitude")
        private float longitude;

        @SerializedName("date")
        private String date;

        @SerializedName("sequence")
        private int sequence;

        // Getters
        public String getPlace_name() { return place_name; }
        public float getLatitude() { return latitude; }
        public float getLongitude() { return longitude; }
        public String getDate() { return date; }
        public int getSequence() { return sequence; }
    }

    // Getters for main class
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public List<RouteData> getData() { return data; }
}