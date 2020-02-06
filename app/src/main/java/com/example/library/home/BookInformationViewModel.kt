package com.example.library.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.library.network.Book
import com.example.library.network.GoodreadsApiService.GoodreadsApi
import com.example.library.network.GoodreadsResponse
import com.example.library.network.Items
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.StringBuilder


class BookInformationViewModel(extractedText: String): ViewModel() {

    private val link: String = "<iframe id=\"the_iframe\" src=\"https://www.goodreads.com/api/reviews_widget_iframe?did=DEVELOPER_ID&amp;format=html&amp;isbn=0689840926&amp;links=660&amp;min_rating=&amp;review_back=fff&amp;stars=000&amp;text=000\" width=\"565\" height=\"400\" frameborder=\"0\"></iframe>"

    private val googleApiKey:String = "AIzaSyDM_5JXY3Ri95ys83rh_Yln6hkkj-VWBEc"

    private val goodreadsApiKey = "g26bxZX7XG4NAXB8PYdzsg"

    private val goodreadsSecret = "NFweT8Zkh84ykbjZXydzTuwPu6qFA5mCWhhzJbawKRI"

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _title = MutableLiveData<String>()

    val title: LiveData<String>
        get() = _title

    private val _subtitle = MutableLiveData<String>()

    val subtitle: LiveData<String>
        get() = _subtitle

    private val _author = MutableLiveData<String>()

    val author:LiveData<String>
        get() = _author

    private val _publisher = MutableLiveData<String>()

    val publisher: LiveData<String>
        get() = _publisher

    private val _description = MutableLiveData<String>()

    val description: LiveData<String>
        get() = _description

    private val _imageLink = MutableLiveData<String>()

    val imageLink: LiveData<String>
        get() = _imageLink

    private val _reviewHtml = MutableLiveData<String>()

    val reviewHtml: LiveData<String>
        get() = _reviewHtml


    init {
        _author.value = ""
        _publisher.value = ""
        _title.value = ""
        _subtitle.value = ""
        _description.value = ""
        apiRequests(extractedText)
    }

    private fun apiRequests(extractedText: String){
        uiScope.launch {
            val id = searchRequest()
            val volume = volumeRequest(id)
            //updateUI(volume)
            val goodreadsResponse = goodReadsRequest(extractedText)
            _reviewHtml.value = link
        }
    }

    private fun updateUI(volume: Book){
        _publisher.value = volume.volumeInfo.publisher
        _title.value = volume.volumeInfo.title
        _description.value = volume.volumeInfo.description
        _subtitle.value = volume.volumeInfo.subtitle
        _imageLink.value = volume.volumeInfo.imageLinks.smallThumbnail
        val sb = StringBuilder()
        for(author in volume.volumeInfo.authors){
            sb.append(author)
        }
        _author.value = sb.toString()

    }
    /*
    searches google books with the text extracted using the url which is a json endpoint
    @return book id, needed to find the specific volume.
     */
    private suspend fun searchRequest(): String {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes?q=1984&maxResults=1&key=$googleApiKey"
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
            val url = "https://www.googleapis.com/books/v1/volumes/$bookId?key=$googleApiKey"
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

    private suspend fun goodReadsRequest(keyword: String): GoodreadsResponse {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf("key" to goodreadsApiKey, "q" to keyword)

            GoodreadsApi.retrofitService.getProperties(parameters)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}