package com.example.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Transformations
import com.example.library.database.AppDatabase
import kotlinx.coroutines.*

class FavoritesViewModel(val userId:Int,
                         val database: AppDatabase,
                         application: Application): AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val favorites = database.favoriteDao.getFavorites(userId)

    val clearButtonVisible = Transformations.map(favorites) {
        it?.isNotEmpty()
    }

    fun onClear() {
        uiScope.launch {
            database.favoriteDao.deleteShelf(userId)
        }
    }

    @Override
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}