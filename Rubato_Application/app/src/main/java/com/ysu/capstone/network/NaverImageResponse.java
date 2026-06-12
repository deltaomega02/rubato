package com.ysu.capstone.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NaverImageResponse {
    @SerializedName("items")
    private List<ImageItem> items;

    public List<ImageItem> getItems() {
        return items;
    }

    public static class ImageItem {
        @SerializedName("link")
        private String link;

        public String getLink() {
            return link;
        }
    }
}
