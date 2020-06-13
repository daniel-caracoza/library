package com.example.library.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.library.ApplicationRepository
import com.example.library.network.GoodreadsResponse
import com.example.library.network.GoogleBook
import java.lang.IllegalArgumentException

class InfoViewModelFactory(private val googleBook: GoogleBook,
                           private val goodreadsResponse: GoodreadsResponse,
                           private val repository: ApplicationRepository): ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(InfoViewModel::class.java)){
            return InfoViewModel(googleBook,goodreadsResponse, repository) as T
        }
        throw IllegalArgumentException("Unkown ViewModel class")
    }

}