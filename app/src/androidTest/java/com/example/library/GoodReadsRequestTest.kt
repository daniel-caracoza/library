package com.example.library

import android.util.Log
import com.example.library.network.GoodreadsApiService.GoodreadsApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.*


@RunWith(AndroidJUnit4::class)
class GoodReadsRequestTest {
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private lateinit var parameters: Map<String, String>
    private val goodreadsApiKey = "g26bxZX7XG4NAXB8PYdzsg"


    @Test
    @Throws(Exception::class)
    fun makeGoodreadsRequestreturnstring(){
        uiScope.launch {
            parameters = mapOf("key" to goodreadsApiKey, "q" to "1984")
            val response = GoodreadsApi.retrofitService.getProperties(parameters)
        }
    }
}
