package com.example.library.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.library.database.User


@Dao
interface UserDao {

    @Query("SELECT * FROM user_table")
    fun getAll(): List<User>

    @Query("SELECT * FROM user_table WHERE username LIKE :userName AND password LIKE :password")
    fun findUser(userName: String, password: String): User?

    @Query("SELECT * FROM user_table WHERE uid LIKE :userid LIMIT 1")
    fun findUserById(userid: Long): User?

    @Query("SELECT * FROM user_table WHERE username LIKE :userName LIMIT 1")
    fun findusername(userName: String): User?

    @Insert
    fun insertAll(user: User)

    @Delete
    fun delete(user: User)
}
