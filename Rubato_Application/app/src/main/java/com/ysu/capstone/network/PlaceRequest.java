package com.ysu.capstone.network;

import java.util.List;

public class PlaceRequest {

    private List<String> selectedLocations;


    public PlaceRequest(List<String> selectedLocations) {
        this.selectedLocations = selectedLocations;
    }

    public List<String> getSelectedLocations() {
        return selectedLocations;
    }

    public void setSelectedLocations(List<String> selectedLocations) {
        this.selectedLocations = selectedLocations;
    }
}
