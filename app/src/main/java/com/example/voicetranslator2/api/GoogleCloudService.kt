package com.example.voicetranslator2.api


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GoogleCloudService {
    @POST(".") // Base URL 뒤에 바로 붙음
    fun processAudio(@Body request: ServerlessRequest): Call<ServerlessResponse>
}