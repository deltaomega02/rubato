package com.ysu.capstone.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import okhttp3.logging.HttpLoggingInterceptor;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "http://YOUR_DB_HOST/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // HTTP 로깅 인터셉터 설정
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttpClient 설정 - 타임아웃 크게 증가
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(180, TimeUnit.SECONDS)     // 3분
                    .readTimeout(180, TimeUnit.SECONDS)        // 3분
                    .writeTimeout(180, TimeUnit.SECONDS)       // 3분
                    .addInterceptor(loggingInterceptor)
                    .retryOnConnectionFailure(true)           // 연결 실패시 재시도
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
