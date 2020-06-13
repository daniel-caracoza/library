package com.example.library.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.library.ApplicationRepository
import com.example.library.network.GoodreadsResponse
import com.example.library.network.GoogleBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class InfoViewModel(googleBook: GoogleBook,
                    goodreadsResponse: GoodreadsResponse,
                    repository: ApplicationRepository): ViewModel(){

    val title = googleBook.volumeInfo.title
    val publishedDate = googleBook.volumeInfo.publishedDate
    val description = googleBook.volumeInfo.description
    val rating = googleBook.volumeInfo.averageRating
    val pageCount = googleBook.volumeInfo.pageCount
    val bookImageUrl = googleBook.volumeInfo.imageLinks.smallThumbnail

    val authorImageUrl = goodreadsResponse.author.image_url
    val authorName = goodreadsResponse.author.name
    val authorBooks = goodreadsResponse.author.books
    val authorBooksList = mutableListOf<String>()
    var about = goodreadsResponse.author.about

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)
    private val repo = repository
    var favoritePressed = MutableLiveData<Boolean?>()

    init {
        authorBooks.forEach {
            authorBooksList.add(it.title)
        }
        cleanAbout()
    }

    fun cleanAbout(){
        about = about.replace("<[^>]*>", " ")
    }


}