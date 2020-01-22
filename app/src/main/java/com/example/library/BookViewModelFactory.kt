package com.example.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class BookViewModelFactory(
    private val extractedText: String): ViewModelProvider.Factory {
    @Suppress("Unchecked cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(BookInformationViewModel::class.java)){
            return BookInformationViewModel(extractedText) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}