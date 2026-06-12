package com.ysu.capstone.network;

import java.util.List;

public class SharedRouteRequest {

    private List<String> regions;

    public SharedRouteRequest(List<String> regions) {
        this.regions = regions;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}
