package com.example.library.network

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

private const val BASE_URL = "https://goodreads.com"

private val tikXml = TikXml.Builder()
    .exceptionOnUnreadXml(false)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(TikXmlConverterFactory.create(tikXml))
    .baseUrl(BASE_URL)
    .build()

interface GoodreadsApiService {
    @GET("search/index.xml")
    suspend fun getSearchProperties(@QueryMap parameters: Map<String, String>): GoodreadsResponse

    @GET("author/show.xml")
    suspend fun getAuthorsBooks(@QueryMap parameters: Map<String, String>): GoodreadsResponse

    @GET("book/show/{id}.xml")
    suspend fun getReviewsAndGenres(@Path("id") id: Int,  @QueryMap parameters: Map<String, String>): GoodreadsResponse

    @GET("book/isbn/{isbn}.xml")
    suspend fun getReviewsAndGenresByISBN(@Path("isbn") isbn: String, @QueryMap parameters: Map<String, String>): GoodreadsResponse


    object GoodreadsApi {

        val retrofitService : GoodreadsApiService by lazy {
            retrofit.create(GoodreadsApiService::class.java)
        }
    }
}