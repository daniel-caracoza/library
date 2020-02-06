package com.example.library.network

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.QueryMap

private const val BASE_URL = "https://goodreads.com"

private const val goodreadsApiKey = "g26bxZX7XG4NAXB8PYdzsg"

private const val goodreadsSecret = "NFweT8Zkh84ykbjZXydzTuwPu6qFA5mCWhhzJbawKRI"

private val tikXml = TikXml.Builder()
    .exceptionOnUnreadXml(false)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(TikXmlConverterFactory.create(tikXml))
    .baseUrl(BASE_URL)
    .build()

interface GoodreadsApiService {
    @GET("search/index.xml")
    suspend fun getProperties(@QueryMap parameters: Map<String, String>): GoodreadsResponse

    @GET("search/index.xml")
    suspend fun getAuthorsBooks(@QueryMap parameters: Map<String, String>): GoodreadsResponse


    object GoodreadsApi {
        val retrofitService : GoodreadsApiService by lazy {
            retrofit.create(GoodreadsApiService::class.java)
        }

    }
}