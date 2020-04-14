package com.example.library.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite WHERE userid LIKE :userid")
    fun getFavorites(userid:Int): LiveData<List<Favorite>>

    @Query("SELECT * FROM favorite WHERE author LIKE :author AND userid LIKE :userid")
    fun filterAuthor(userid:Int, author:String):LiveData<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg favorites: Favorite)

    @Query("DELETE FROM favorite WHERE bookid LIKE :bookid AND userid LIKE :userid")
    fun deleteFavorite(userid:Int, bookid: String)

    @Query("DELETE FROM favorite WHERE userid LIKE :userId")
    suspend fun deleteShelf(userId: Int)

}