package com.ysu.capstone.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NaverApiClient {
    private static final String BASE_URL = "https://openapi.naver.com/";



    public static NaverApiService getApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(NaverApiService.class);
    }
}
