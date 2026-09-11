package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class RouteSaveRequest {
    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("total_days")
    private int totalDays;

    @SerializedName("areas")
    private List<String> areas;

    @SerializedName("total_distance")
    private double totalDistance;

    @SerializedName("route_details")
    private List<DayRouteDetail> routeDetails;

    @SerializedName("route_image")
    private String routeImage;

    // Getters and Setters
    public String getUserEmail() { return userEmail; }
    public int getTotalDays() { return totalDays; }
    public List<String> getAreas() { return areas; }
    public double getTotalDistance() { return totalDistance; }
    public List<DayRouteDetail> getRouteDetails() { return routeDetails; }
    public String getRouteImage() { return routeImage; }

    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public void setAreas(List<String> areas) { this.areas = areas; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    public void setRouteDetails(List<DayRouteDetail> routeDetails) { this.routeDetails = routeDetails; }
    public void setRouteImage(String routeImage) { this.routeImage = routeImage; }

    public static class DayRouteDetail {
        @SerializedName("day")
        private int day;

        @SerializedName("date")
        private String date;

        @SerializedName("places")
        private List<PlaceDetail> places;

        @SerializedName("distances")
        private List<String> distances;

        // Getters and Setters
        public int getDay() { return day; }
        public String getDate() { return date; }
        public List<PlaceDetail> getPlaces() { return places; }
        public List<String> getDistances() { return distances; }

        public void setDay(int day) { this.day = day; }
        public void setDate(String date) { this.date = date; }
        public void setPlaces(List<PlaceDetail> places) { this.places = places; }
        public void setDistances(List<String> distances) { this.distances = distances; }

        // Constructor
        public DayRouteDetail() {
            this.places = new ArrayList<>();
            this.distances = new ArrayList<>();
        }
    }

    public static class PlaceDetail {
        @SerializedName("place_name")
        private String placeName;

        @SerializedName("area_name")
        private String areaName;

        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("sequence")
        private int sequence;

        // Getters and Setters
        public String getPlaceName() { return placeName; }
        public String getAreaName() { return areaName; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getSequence() { return sequence; }

        public void setPlaceName(String placeName) { this.placeName = placeName; }
        public void setAreaName(String areaName) { this.areaName = areaName; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public void setSequence(int sequence) { this.sequence = sequence; }
    }
}