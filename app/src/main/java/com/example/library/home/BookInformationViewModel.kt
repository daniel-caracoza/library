package com.example.library.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.library.BuildConfig
import com.example.library.database.Favorite
import com.example.library.database.FavoriteDao
import com.example.library.network.GoogleBook
import com.example.library.network.GoodreadsApiService.GoodreadsApi
import com.example.library.network.GoodreadsResponse
import com.example.library.network.Items
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.StringBuilder


class BookInformationViewModel(val userId:Int, val database: FavoriteDao, application: Application,
                               extractedText: String): AndroidViewModel(application) {

    private val googleApiKey = BuildConfig.googleKey

    private val goodreadsApiKey = BuildConfig.goodreadsKey

    private val goodreadsSecret = BuildConfig.goodreadsSecret

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _title = MutableLiveData<String>()

    val title: LiveData<String>
        get() = _title

    private var bookid: Int = 0

    private lateinit var image: String

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
            //val id = searchRequest()
            //val volume = volumeRequest(id)
            //updateUI(volume)
            val goodreadsSearchResponse = goodReadsSearchRequest(extractedText)
            //val goodreadsAuthorsBooksResponse = goodReadsAuthorBooksRequest(goodreadsSearchResponse.search.works[0].best_book.authorId)
            val goodreadsReviewsAndGenres = goodReadsReviewsAndGenresRequest(goodreadsSearchResponse.search.works[0].best_book.id)
            _description.value = goodreadsReviewsAndGenres.book.description
            image = goodreadsReviewsAndGenres.book.small_image_url
            updateUIGRApi(goodreadsSearchResponse)
            //_reviewHtml.value = link
        }
    }

    private fun updateUI(volume: GoogleBook){
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

    private fun updateUIGRApi(response: GoodreadsResponse){
        _title.value = response.search.works[0].best_book.title
        _author.value = response.search.works[0].best_book.name
        bookid = response.search.works[0].best_book.id
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
    private suspend fun volumeRequest(bookId: String): GoogleBook {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes/$bookId?key=$googleApiKey"
            val json = fetchJSON(url)
            val gson = GsonBuilder().create()
            val volume = gson.fromJson(json, GoogleBook::class.java)

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
    /*
    @return a GoodsreadsResponse which contains a list of works(books) author name & id.
    @param the extracted text from the camera view.
     */
    private suspend fun goodReadsSearchRequest(keyword: String): GoodreadsResponse {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf("key" to goodreadsApiKey, "q" to keyword)

            GoodreadsApi.retrofitService.getSearchProperties(parameters)
        }
    }
    /*
    @return a GoodsreadsResponse which contains a list of books by author
    @param the author id
     */
    private suspend fun goodReadsAuthorBooksRequest(authorId: String): GoodreadsResponse {
        return withContext(Dispatchers.IO){
            val parameters = mapOf<String, String>("key" to goodreadsApiKey, "id" to authorId)
            GoodreadsApi.retrofitService.getAuthorsBooks(parameters)
        }
    }
    /*@return a GoodreadsResponse which contains a list of genres the book belongs to and a review widget in raw html as an iframe, you
     *you need to display on a webview.
     */
    private suspend fun goodReadsReviewsAndGenresRequest(id: Int): GoodreadsResponse {
        return withContext(Dispatchers.IO){
            val parameters = mapOf<String, String>("key" to goodreadsApiKey)
            GoodreadsApi.retrofitService.getReviewsAndGenres(id, parameters)
        }
    }

    private suspend fun goodReadsReviewsAndGenresRequestByISBN(isbn: String): GoodreadsResponse {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf<String, String>("key" to goodreadsApiKey)
            GoodreadsApi.retrofitService.getReviewsAndGenresByISBN(isbn, parameters)
        }
    }
    fun addFavoriteStart(){
        uiScope.launch {
            addFavorite()
        }

    }
    private suspend fun addFavorite(){
        withContext(Dispatchers.IO){
            val favorite = Favorite(bookid, userId, _author.value!!, title.value!!, _description.value!!, image)
            database.insertAll(favorite)
        }


    }
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}