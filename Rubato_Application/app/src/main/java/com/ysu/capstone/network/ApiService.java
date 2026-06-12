package com.ysu.capstone.network;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    // 기존 메소드들...

    // 로그인 요청
    @POST("login.php")
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    //회원가입 요청
    @POST("register.php")
    Call<RegisterResponse> registerUser(@Body RegisterRequest registerRequest);

    //이메일 중복 확인 요청
    @POST("check_email.php")
    Call<ResponseBody> checkEmail(@Body EmailRequest emailRequest);

    // ChatGPT 요청
    @Headers("Content-Type: application/json")
    @POST("chat_main.php")
    Call<ResponseBody> sendChatRequest(@Body ChatRequest chatRequest);

    // ChatGPT 실시간 반응 요청
    @Headers("Content-Type: application/json")
    @POST("recomment.php")
    Call<ResponseBody> locationRecomment(@Body RecommentRequest recommentRequest);

    // 관광지 리스트 요청
    @Headers("Content-Type: application/json")
    @POST("get_place.php")
    Call<PlaceResponse> getPlaces(@Body PlaceRequest placeRequest);

    // Shared Route 리스트 요청
    @Headers("Content-Type: application/json")
    @POST("shared_route.php")
    Call<SharedRouteResponse> getSharedRoutes(@Body SharedRouteRequest request);

    @Headers("Content-Type: application/json")
    @POST("save_route_3.php")
    Call<RouteSaveResponse> analyzeSaveRoute(@Body RouteSaveRequest request);

    // 테마별 루트 요청
    @Headers("Content-Type: application/json")
    @POST("theme_route.php")
    Call<ThemeRouteResponse> getThemeRoutes(@Body ThemeRouteRequest request);

    // 지역별 루트 요청 추가
    @Headers("Content-Type: application/json")
    @POST("area_route.php")
    Call<AreaRouteResponse> getAreaRoutes(@Body AreaRouteRequest request);

    @Headers("Content-Type: application/json")
    @POST("auto_route_new.php")
    Call<AutoRouteResponse> getAutoRoute(@Body AutoRouteRequest request);

    @Headers("Content-Type: application/json")
    @POST("get_route_details.php")
    Call<RouteDetailResponse> getRouteDetails(@Body RouteDetailRequest request);

    // 사용자 정보 업데이트 요청
    @Headers("Content-Type: application/json")
    @POST("update_user_info.php")
    Call<UpdateUserResponse> updateUserInfo(@Body UpdateUserRequest updateUserRequest);

    //중복확인
    @POST("check_email.php")
    Call<ResponseBody> verifyCode(@Body Map<String, String> request);

    //이메일 처리
    @POST("check_email.php")
    Call<ResponseBody> checkEmail(@Body Map<String, String> request);
}
