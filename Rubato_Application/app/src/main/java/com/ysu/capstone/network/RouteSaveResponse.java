package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class RouteSaveResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("route_summary")
    private RouteSummary routeSummary;

    public static class RouteSummary {
        @SerializedName("route_id")
        private int routeId;

        @SerializedName("theme")
        private String theme;

        @SerializedName("tags")
        private List<String> tags;

        @SerializedName("total_cost")
        private double totalCost;

        @SerializedName("daily_costs")
        private List<Double> dailyCosts;

        @SerializedName("total_distance")
        private double totalDistance;

        @SerializedName("route_score")
        private int routeScore;

        @SerializedName("image_saved")
        private boolean imageSaved;

        @SerializedName("daily_details")
        private List<DailyDetail> dailyDetails;

        // Getters
        public int getRouteId() { return routeId; }
        public String getTheme() { return theme; }
        public List<String> getTags() { return tags; }
        public double getTotalCost() { return totalCost; }
        public List<Double> getDailyCosts() { return dailyCosts; }
        public double getTotalDistance() { return totalDistance; }
        public int getRouteScore() { return routeScore; }
        public boolean isImageSaved() { return imageSaved; }
        public List<DailyDetail> getDailyDetails() { return dailyDetails; }
    }

    public static class DailyDetail {
        @SerializedName("day")
        private int day;

        @SerializedName("date")
        private String date;

        @SerializedName("places")
        private List<PlaceDetail> places;

        @SerializedName("day_total_cost")
        private double dayTotalCost;

        @SerializedName("distances")
        private List<String> distances;

        @SerializedName("total_distance")
        private double totalDistance;

        // Getters
        public int getDay() { return day; }
        public String getDate() { return date; }
        public List<PlaceDetail> getPlaces() { return places; }
        public double getDayTotalCost() { return dayTotalCost; }
        public List<String> getDistances() { return distances != null ? distances : new ArrayList<>(); }
        public double getTotalDistance() { return totalDistance; }
    }

    public static class PlaceDetail {
        @SerializedName("name")
        private String name;

        @SerializedName("estimated_cost")
        private double estimatedCost;

        // Getters
        public String getName() { return name; }
        public double getEstimatedCost() { return estimatedCost; }
    }

    // Getters
    public String getStatus() { return status; }
    public RouteSummary getRouteSummary() { return routeSummary; }

    // 편의 메서드들
    public int getRouteId() {
        return routeSummary != null ? routeSummary.getRouteId() : 0;
    }

    public String getTheme() {
        return routeSummary != null ? routeSummary.getTheme() : null;
    }

    public List<String> getTags() {
        return routeSummary != null ? routeSummary.getTags() : null;
    }

    public double getTotalDistance() {
        return routeSummary != null ? routeSummary.getTotalDistance() : 0.0;
    }

    public double getEstimatedCost() {
        return routeSummary != null ? routeSummary.getTotalCost() : 0.0;
    }

    public int getRouteScore() {
        return routeSummary != null ? routeSummary.getRouteScore() : 0;
    }

    public boolean isImageSaved() {
        return routeSummary != null && routeSummary.isImageSaved();
    }
}