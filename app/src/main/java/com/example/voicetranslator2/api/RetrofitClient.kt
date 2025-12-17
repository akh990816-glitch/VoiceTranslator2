package com.example.voicetranslator2.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 3개의 API 주소가 조금씩 다르지만, Retrofit은 기본 URL이 필수입니다.
    // 어차피 Interface에서 @Url로 덮어씌울 거라 기본값만 설정합니다.
    private const val BASE_URL = "https://translator-backend-1008469748086.asia-northeast3.run.app"

    val service: GoogleCloudService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON을 자동으로 DTO로 변환
            .build()
            .create(GoogleCloudService::class.java)
    }
}