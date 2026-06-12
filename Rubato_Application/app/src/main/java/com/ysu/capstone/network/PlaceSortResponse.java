package com.ysu.capstone.network;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class PlaceSortResponse {
    private String status;
    private Map<String, List<String>> sortedPlaces;

    public PlaceSortResponse() {
        // 기본 생성자
        this.sortedPlaces = new HashMap<>();  // null 방지를 위한 초기화
    }

    public String getStatus() {
        return status != null ? status : "";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, List<String>> getSortedPlaces() {
        return sortedPlaces != null ? sortedPlaces : new HashMap<>();
    }

    public void setSortedPlaces(Map<String, List<String>> sortedPlaces) {
        this.sortedPlaces = sortedPlaces;
    }

    @Override
    public String toString() {
        return "PlaceSortResponse{" +
                "status='" + status + '\'' +
                ", sortedPlaces=" + sortedPlaces +
                '}';
    }
}
