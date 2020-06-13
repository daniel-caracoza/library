package com.example.library.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

private const val BASE_URL  = "https://www.googleapis.com/books/v1/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

interface GoogleBooksApiService {

    @GET("volumes/{volumeId}")
    suspend fun getVolume(@Path("volumeId") volumeId: String, @QueryMap parameters: Map<String, String>): GoogleBook

    @GET("volumes")
    suspend fun performSearch(@QueryMap parameters: Map<String, String>): Items

    @GET("volumes")
    suspend fun performBookSearch(@QueryMap parameters: Map<String, String>): Items

    object GoogleBooksApi {
        val retrofitService: GoogleBooksApiService by lazy {
            retrofit.create(GoogleBooksApiService::class.java)
        }
    }
}