package com.example.library.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.library.database.User


@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE username LIKE :userName AND password LIKE :password LIMIT 1")
    suspend fun findUser(userName: String, password: String): User

    @Query("SELECT * FROM user WHERE uid LIKE :userid LIMIT 1")
    suspend fun findUserById(userid: Int): User

    @Query("SELECT * FROM user WHERE username LIKE :userName LIMIT 1")
    suspend fun findusername(userName: String): User

    @Insert
    suspend fun insertAll(vararg users: User)

    @Delete
    suspend fun delete(user: User)
}
