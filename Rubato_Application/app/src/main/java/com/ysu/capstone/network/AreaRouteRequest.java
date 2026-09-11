package com.ysu.capstone.network;

public class AreaRouteRequest {
    private String area_name;

    public AreaRouteRequest(String area_name) {
        this.area_name = area_name;
    }

    public String getArea_name() {
        return area_name;
    }

    public void setArea_name(String area_name) {
        this.area_name = area_name;
    }
}