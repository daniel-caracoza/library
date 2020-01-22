package com.example.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.StringBuilder

class BookInformationViewModel(extractedText: String): ViewModel() {

    private val apiKey:String = "AIzaSyDM_5JXY3Ri95ys83rh_Yln6hkkj-VWBEc"

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _title = MutableLiveData<String>()

    val title: LiveData<String>
        get() = _title

    private val _author = MutableLiveData<String>()

    val author:LiveData<String>
        get() = _author

    private val _publisher = MutableLiveData<String>()

    val publisher: LiveData<String>
        get() = _publisher

    init {
        _author.value = ""
        _publisher.value = ""
        _title.value = ""
        apiRequests()
    }

    private fun apiRequests(){
        uiScope.launch {
            val id = searchRequest()
            val volume = volumeRequest(id)
            updateUI(volume)
        }
    }

    private fun updateUI(volume: Book){
        _publisher.value = volume.volumeInfo.publisher
        _title.value = volume.volumeInfo.title
        val sb = StringBuilder()
        for(author in volume.volumeInfo.authors){
            sb.append("$author ")
        }
        _author.value = sb.toString()

    }
    /*
    searches google books with the text extracted using the url which is a json endpoint
    @return book id, needed to find the specific volume.
     */
    private suspend fun searchRequest(): String {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes?q=1984&maxResults=1&key=$apiKey"
            val json = fetchJSON(url)
            val gson = GsonBuilder().create()
            val books = gson.fromJson(json, Items::class.java)
            //verified grabs a book id
            books.items[0].id
        }
    }
    /*
    searches api with a volume id and grab the information needed to update UI
     */
    private suspend fun volumeRequest(bookId: String): Book {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes/$bookId?key=$apiKey"
            val json = fetchJSON(url)
            val gson = GsonBuilder().create()
            val volume = gson.fromJson(json, Book::class.java)

            volume
        }

    }
    /*
    @return a json object as a string
     */
    private suspend fun fetchJSON(url: String): String? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val client = OkHttpClient()
            val response: Response = client.newCall(request).execute()

            response.body?.string()
        }
    }
}