package com.example.library.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

var BaseUrl = "https://api.nytimes.com/svc/books/v3/"

val retrofit2 = Retrofit.Builder()
    .baseUrl(BaseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface BooksService {

    @GET("lists/best-sellers/history.json?author")
    fun getNYTimesBestSellerDataByAuthor(@Query("api-key") apikey: String, @Query("author") author: String): BooksResponse

    @GET("lists/best-sellers/history.json?title")
    fun getNYTimesBestSellerDataByTitle(@Query("api-key") apikey: String, @Query("title") title: String): BooksResponse

    @GET("lists/best-sellers/history.json?isbn")
    fun getNYTimesBestSellerDataByIsbn(@Query("api-key") apikey: String, @Query("isbn") isbn: String): BooksResponse

    object NYTimesApi{
        val service = retrofit2.create(BooksService::class.java)
    }
}