package com.example.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE username LIKE :userName " +
            "AND password LIKE :password LIMIT 1")

    fun findUser(userName: String, password: String): User

    @Query("SELECT * FROM user WHERE username LIKE :userName LIMIT 1")
    fun findusername(userName: String): User

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(user: User)
}
