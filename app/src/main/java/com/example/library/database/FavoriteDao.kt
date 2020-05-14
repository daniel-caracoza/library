package com.example.library.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite WHERE userid LIKE :userid")
    fun getFavorites(userid:Long): LiveData<List<Favorite>>

    @Query("SELECT * FROM favorite WHERE author LIKE :author AND userid LIKE :userid")
    fun filterAuthor(userid:Long, author:String):LiveData<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg favorites: Favorite)

    @Query("DELETE FROM favorite WHERE bookid LIKE :bookid AND userid LIKE :userid")
    fun deleteFavorite(userid:Long, bookid: String)

    @Query("DELETE FROM favorite")
    fun deleteShelf()

}