package com.example.library

import android.util.Log
import com.example.library.network.GoodreadsApiService.GoodreadsApi
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test
    @Throws(Exception::class)
    fun makeGoodreadsRequestreturnstring(){
        var response: GoogleBook? = null
        val volumeid = "zyTCAlFPjgYC"
        parameters = mapOf("key" to googleKey)
        response = runBlocking {GoogleBooksApiService.GoogleBooksApi.retrofitService.getVolume(volumeid, parameters)}
        assertThat("Business & Economics / Entrepreneurship", equalTo(response!!.volumeInfo.categories[0]))
    }

    @After
    fun destroy(){
        viewModelJob.cancel()
    }
}