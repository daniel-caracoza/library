package com.example.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.library.database.FavoriteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class FavoritesViewModel(

    val userId:Int,

    val database: FavoriteDao,
    application: Application): AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val favorites = database.getFavorites(userId)

    @Override
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}