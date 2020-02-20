package com.example.library.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.library.database.FavoriteDao
import java.lang.IllegalArgumentException

class BookViewModelFactory(
    private val userId:Int,
    private val dataSource: FavoriteDao,
    private val application: Application,
    private val extractedText: String): ViewModelProvider.Factory {
    @Suppress("Unchecked cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(BookInformationViewModel::class.java)){
            return BookInformationViewModel(
                userId,
                dataSource,
                application,
                extractedText
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}