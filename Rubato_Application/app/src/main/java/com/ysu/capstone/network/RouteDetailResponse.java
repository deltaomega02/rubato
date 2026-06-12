package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RouteDetailResponse {
    @SerializedName("success")
    private boolean success;  // 요청 성공 여부

    @SerializedName("details")
    private List<RouteDetail> details;  // Route_detail 테이블의 데이터 리스트

    @SerializedName("areas")
    private List<String> areaNames;  // 지역명 리스트

    // Getter 및 Setter
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<RouteDetail> getDetails() {
        return details;
    }

    public void setDetails(List<RouteDetail> details) {
        this.details = details;
    }

    public List<String> getAreaNames() {
        return areaNames;
    }

    public void setAreaNames(List<String> areaNames) {
        this.areaNames = areaNames;
    }

    // RouteDetail 내부 클래스
    public static class RouteDetail {
        @SerializedName("route_detail_id")
        private int route_detail_id;  // Route_detail ID

        @SerializedName("route_id")
        private int route_id;  // Route ID

        @SerializedName("place_name")
        private String place_name;  // 장소 이름

        @SerializedName("place_latitude")
        private double place_latitude;  // 장소 위도

        @SerializedName("place_longitude")
        private double place_longitude;  // 장소 경도

        @SerializedName("date")
        private String date;  // 날짜

        @SerializedName("route_seq")
        private int route_seq;  // 순서

        // Getter 및 Setter
        public int getRouteDetailId() {
            return route_detail_id;
        }

        public void setRouteDetailId(int route_detail_id) {
            this.route_detail_id = route_detail_id;
        }

        public int getRouteId() {
            return route_id;
        }

        public void setRouteId(int route_id) {
            this.route_id = route_id;
        }

        public String getPlaceName() {
            return place_name;
        }

        public void setPlaceName(String place_name) {
            this.place_name = place_name;
        }

        public double getPlaceLatitude() {
            return place_latitude;
        }

        public void setPlaceLatitude(double place_latitude) {
            this.place_latitude = place_latitude;
        }

        public double getPlaceLongitude() {
            return place_longitude;
        }

        public void setPlaceLongitude(double place_longitude) {
            this.place_longitude = place_longitude;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getRouteSeq() {
            return route_seq;
        }

        public void setRouteSeq(int route_seq) {
            this.route_seq = route_seq;
        }

        @Override
        public String toString() {
            return "RouteDetail{" +
                    "route_detail_id=" + route_detail_id +
                    ", route_id=" + route_id +
                    ", place_name='" + place_name + '\'' +
                    ", place_latitude=" + place_latitude +
                    ", place_longitude=" + place_longitude +
                    ", date='" + date + '\'' +
                    ", route_seq=" + route_seq +
                    '}';
        }
    }
}
