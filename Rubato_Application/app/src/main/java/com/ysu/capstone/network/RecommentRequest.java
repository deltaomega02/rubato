package com.ysu.capstone.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RecommentRequest {
    @SerializedName("location_name")
    @Expose
    private String location_name;

    // 생성자 및 Getter, Setter
    public RecommentRequest(String location_name) {
        this.location_name = location_name;
    }

    public String getLocation_name() {
        return location_name;
    }

    public void setLocation_name(String location_name) {
        this.location_name = location_name;
    }

    @Override
    public String toString() {
        return "RecommentRequest{" +
                "location_name='" + location_name + '\'' +
                '}';
    }
}
