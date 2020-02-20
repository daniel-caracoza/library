package com.example.library.favorites

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.library.database.FavoriteDao
import java.lang.IllegalArgumentException

class FavoritesViewModelFactory(
    private val userId: Int,
    private val dataSource: FavoriteDao,
    private val application: Application):ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FavoritesViewModel::class.java)){
            return FavoritesViewModel(userId, dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
