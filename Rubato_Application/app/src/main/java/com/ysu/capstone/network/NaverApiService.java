package com.ysu.capstone.network;

import com.ysu.capstone.network.NaverImageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverApiService {
    @GET("v1/search/image")
    Call<NaverImageResponse> searchImages(
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query,
            @Query("display") int display,
            @Query("start") int start,
            @Query("sort") String sort
    );
}
