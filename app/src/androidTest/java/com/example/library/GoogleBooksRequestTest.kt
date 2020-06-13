package com.example.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.library.network.GoodreadsApiService
import com.example.library.network.GoodreadsResponse
import com.example.library.network.GoogleBook
import com.example.library.network.GoogleBooksApiService
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.*


@RunWith(AndroidJUnit4::class)
class GoogleBooksRequestTest {
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private lateinit var parameters: Map<String, String>
    private val googleKey = BuildConfig.googleKey
    private val goodreadsKey = BuildConfig.goodreadsKey

    @Test
    @Throws(Exception::class)
    fun makeGoogleRequestreturnstring(){
        var response: GoogleBook? = null
        val volumeid = "zyTCAlFPjgYC"
        parameters = mapOf("key" to googleKey)
        response = runBlocking {GoogleBooksApiService.GoogleBooksApi.retrofitService.getVolume(volumeid, parameters)}
        assertThat("Business & Economics / Entrepreneurship", equalTo(response!!.volumeInfo.categories[0]))
    }

    @Test
    @Throws(Exception::class)
    fun makeGoodReadsRequestReturnId(){
        var response: GoodreadsResponse? = null
        val name: String = "george orwell"
        parameters = mapOf("key" to goodreadsKey)
        response = runBlocking { GoodreadsApiService.GoodreadsApi.retrofitService.getAuthorId(name, parameters) }
        assertThat<Int>(3706, equalTo(response!!.author.id))
    }

    @Test
    @Throws(Exception::class)
    fun makeGoodReadsRequestReturnAuthor(){
        var response: GoodreadsResponse? = null
        val id = "3706"
        parameters = mapOf("key" to goodreadsKey)
        response = runBlocking { GoodreadsApiService.GoodreadsApi.retrofitService.getAuthorsBooks(id, parameters) }
        assertThat("George Orwell", equalTo(response!!.author.name))
    }

    @After
    fun destroy(){
        viewModelJob.cancel()
    }
}