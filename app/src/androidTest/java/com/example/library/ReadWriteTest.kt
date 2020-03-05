package com.example.library

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.database.UserDao
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ReadWriteTest {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDB(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        userDao = db.userDao()
    }
    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList(){
        val user: User =
            User(
                1,
                "test",
                "test@domain.com",
                "test"
            )
        userDao.insertAll(user)
        val users = userDao.getAll()
        assertThat(user, equalTo(users[0]))
    }
    @Test
    @Throws(Exception::class)
    fun checkIfUsernameExists(){
        val user: User =
            User(
                1,
                "test",
                "test@domain.com",
                "test"
            )
        userDao.insertAll(user)
        val foundUser = userDao.findusername(user.userName!!)
        assertThat(foundUser, equalTo(user))
    }
    @After
    fun closeDB(){
        db.close()
    }
}
