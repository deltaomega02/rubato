package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SharedRouteResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<Route> data;

    @SerializedName("total_count")
    private int totalCount;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<Route> getData() {
        return data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public static class Route {

        @SerializedName("route_id")
        private int routeId;

        @SerializedName("user_name")
        private String userName;

        @SerializedName("likes")
        private int likes;

        @SerializedName("route_count")
        private int routeCount;

        @SerializedName("route_distance")
        private Double routeDistance;

        @SerializedName("route_expense")
        private String routeExpense;

        @SerializedName("route_details")
        private List<RouteDetail> routeDetails;

        @SerializedName("route_image")
        private String routeImage;

        public int getRouteId() {
            return routeId;
        }

        public String getUserName() {
            return userName;
        }

        public int getLikes() {
            return likes;
        }

        public int getRouteCount() {
            return routeCount;
        }

        public Double getRouteDistance() {
            return routeDistance;
        }

        public String getRouteExpense() {
            return routeExpense;
        }

        public List<RouteDetail> getRouteDetails() {
            return routeDetails;
        }

        public String getRouteImage() {
            return routeImage;
        }

        public void setRouteImage(String routeImage) {
            this.routeImage = routeImage;
        }
    }

    public static class RouteDetail {
        @SerializedName("route_detail_id")
        private int routeDetailId;

        @SerializedName("place_name")
        private String placeName;

        @SerializedName("place_latitude")
        private double placeLatitude;

        @SerializedName("place_longitude")
        private double placeLongitude;

        @SerializedName("date")
        private String date;

        @SerializedName("route_seq")
        private int routeSeq;

        public int getRouteDetailId() {
            return routeDetailId;
        }

        public String getPlaceName() {
            return placeName;
        }

        public double getPlaceLatitude() {
            return placeLatitude;
        }

        public double getPlaceLongitude() {
            return placeLongitude;
        }

        public String getDate() {
            return date;
        }

        public int getRouteSeq() {
            return routeSeq;
        }
    }
}
