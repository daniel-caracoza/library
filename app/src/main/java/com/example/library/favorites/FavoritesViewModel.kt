package com.example.library.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Transformations
import com.example.library.database.FavoriteDao
import kotlinx.coroutines.*

class FavoritesViewModel(

    userId:Int,
    val database: FavoriteDao,
    application: Application): AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val favorites = database.getFavorites(userId)

    val clearButtonVisible = Transformations.map(favorites) {
        it?.isNotEmpty()
    }

    fun onClear(){
        uiScope.launch {
            clear()
        }
    }

    private suspend fun clear(){
        withContext(Dispatchers.IO){
            database.deleteShelf()
        }
    }

    @Override
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}