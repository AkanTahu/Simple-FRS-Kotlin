package com.example.cobaalt1

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/recognize")
    fun uploadImage(
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @Multipart
    @POST("/register")
    fun registerFace(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody
    ): Call<ResponseBody>

}