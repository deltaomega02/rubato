package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AutoRouteRequest {
    @SerializedName("selected_locations")
    private List<String> selectedLocations;  // 선택한 지역 리스트

    @SerializedName("selected_tags")
    private List<String> selectedTags;       // 선택한 태그 리스트

    @SerializedName("num_days")
    private int numberOfDays;                // 여행 일수

    @SerializedName("num_nights")
    private int numberOfNights;              // 숙박 일수

    @SerializedName("start_date")
    private String startDate;     // 시작일

    @SerializedName("end_date")
    private String endDate;       // 종료일


    public AutoRouteRequest(
            List<String> selectedLocations,
            List<String> selectedTags,
            int numberOfDays,
            int numberOfNights,
            String startDate,
            String endDate
    ) {
        this.selectedLocations = selectedLocations;
        this.selectedTags = selectedTags;
        this.numberOfDays = numberOfDays;
        this.numberOfNights = numberOfNights;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}