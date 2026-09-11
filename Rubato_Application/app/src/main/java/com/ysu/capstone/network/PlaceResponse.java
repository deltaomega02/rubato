package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PlaceResponse {
    @SerializedName("place_names")
    private List<String> place_names;

    @SerializedName("place_addresses")
    private List<String> place_addresses;

    @SerializedName("latitudes")
    private List<Double> latitudes;

    @SerializedName("longitudes")
    private List<Double> longitudes;

    @SerializedName("place_type")
    private List<String> place_type;

    @SerializedName("tags")
    private List<List<String>> tags;

    // 기본 생성자
    public PlaceResponse() {}

    // Getter 및 Setter
    public List<String> getPlaceNames() {
        return place_names;
    }

    public void setPlaceNames(List<String> place_names) {
        this.place_names = place_names;
    }

    public List<String> getPlaceAddresses() {
        return place_addresses;
    }

    public void setPlaceAddresses(List<String> place_addresses) {
        this.place_addresses = place_addresses;
    }

    public List<Double> getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(List<Double> latitudes) {
        this.latitudes = latitudes;
    }

    public List<Double> getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(List<Double> longitudes) {
        this.longitudes = longitudes;
    }

    public List<String> getPlaceType() {
        return place_type;
    }

    public void setPlaceType(List<String> place_type) {
        this.place_type = place_type;
    }

    public List<List<String>> getTags() {
        return tags;
    }

    public void setTags(List<List<String>> tags) {
        this.tags = tags;
    }
}