package com.ysu.capstone.network;

import java.util.List;

public class ThemeRouteRequest {
    private List<String> themes;

    public ThemeRouteRequest(List<String> themes) {
        this.themes = themes;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }
}