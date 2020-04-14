package com.example.library.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.library.ApplicationRepository
import com.example.library.database.AppDatabase
import com.example.library.database.Favorite
import com.example.library.network.GoogleBook
import com.example.library.network.GoodreadsResponse
import kotlinx.coroutines.*
import java.lang.StringBuilder


class BookInformationViewModel(val userId:Int, val database: AppDatabase, application: Application,
                               extractedText: String): AndroidViewModel(application) {

    private val applicationRepository = ApplicationRepository(database)

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _title = MutableLiveData<String>()

    val title: LiveData<String>
        get() = _title

    private var bookid: Int = 0

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
            val id = applicationRepository.googleBooksSearchRequest(extractedText).items[0].id
            val volume = applicationRepository.googleBooksGetVolume(id)
            updateUI(volume)
            //val goodreadsSearchResponse = applicationRepository.goodReadsSearchRequest(extractedText)
            //val goodreadsAuthorsBooksResponse = goodReadsAuthorBooksRequest(goodreadsSearchResponse.search.works[0].best_book.authorId)
            //val goodreadsReviewsAndGenres = applicationRepository.goodReadsReviewsAndGenresRequest(goodreadsSearchResponse.search.works[0].best_book.id)
            //updateUIGRApi(goodreadsSearchResponse)
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

    fun addFavoriteStart(){
        uiScope.launch {
            val favorite = Favorite(bookid, userId, _author.value!!, title.value!!, _description.value!!, _imageLink.value!!)
            applicationRepository.addFavorite(favorite)
        }

    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}