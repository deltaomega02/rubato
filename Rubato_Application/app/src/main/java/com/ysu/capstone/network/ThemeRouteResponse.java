package com.ysu.capstone.network;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ThemeRouteResponse {
    private String status;
    private String message;
    private List<RouteData> data;

    public static class RouteData {
        private int route_id;
        private String user_name;
        private int likes;
        private int final_score;
        private String theme_name;
        private double estimated_cost;
        private double total_distance;
        private List<String> areas;
        private List<String> tags;
        private List<RouteDetail> route_details;
        @SerializedName("route_image")
        private String routeImage;

        public int getRoute_id() { return route_id; }
        public void setRoute_id(int route_id) { this.route_id = route_id; }

        public String getUser_name() { return user_name; }
        public void setUser_name(String user_name) { this.user_name = user_name; }

        public int getLikes() { return likes; }
        public void setLikes(int likes) { this.likes = likes; }

        public int getFinal_score() { return final_score; }
        public void setFinal_score(int final_score) { this.final_score = final_score; }

        public String getTheme_name() { return theme_name; }
        public void setTheme_name(String theme_name) { this.theme_name = theme_name; }

        public double getEstimated_cost() { return estimated_cost; }
        public void setEstimated_cost(double estimated_cost) { this.estimated_cost = estimated_cost; }

        public double getTotal_distance() { return total_distance; }
        public void setTotal_distance(double total_distance) { this.total_distance = total_distance; }

        public List<String> getAreas() { return areas; }
        public void setAreas(List<String> areas) { this.areas = areas; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public List<RouteDetail> getRoute_details() { return route_details; }
        public void setRoute_details(List<RouteDetail> route_details) { this.route_details = route_details; }

        public String getRouteImage() { return routeImage; }
        public void setRouteImage(String routeImage) { this.routeImage = routeImage; }
    }

    public static class RouteDetail {
        @SerializedName("place_name")
        private String place_name;

        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("date")
        private String date;

        @SerializedName("sequence")
        private Integer sequence; // int를 Integer로 변경하여 null 허용

        public String getPlace_name() { return place_name; }
        public void setPlace_name(String place_name) { this.place_name = place_name; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getDate() {
            // date가 null이고 sequence가 유효한 경우에만 날짜를 계산
            if (date == null && sequence != null && sequence > 0) {
                // 필요한 경우 여기서 날짜 계산 로직 추가
            }
            return date;
        }
        public void setDate(String date) { this.date = date; }

        public Integer getSequence() { return sequence; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }

        @Override
        public String toString() {
            return String.format("RouteDetail{place_name='%s', latitude=%f, longitude=%f, date='%s', sequence=%d}",
                    place_name, latitude, longitude, date, sequence);
        }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<RouteData> getData() { return data; }
    public void setData(List<RouteData> data) { this.data = data; }
}