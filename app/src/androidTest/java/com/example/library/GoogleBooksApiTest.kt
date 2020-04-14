package com.example.library

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.library.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleBooksApiTest {

    val job = Job()
    val uiScope = CoroutineScope(Dispatchers.Main + job)
    lateinit var applicationRepository: ApplicationRepository
    lateinit var database:AppDatabase

    @Before
    fun createDB(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        applicationRepository = ApplicationRepository(database)
    }

    @Test
    @Throws(Exception::class)
    fun googleBooksSearchRequestTest(){
        uiScope.launch {
            applicationRepository.googleBooksSearchRequest("1984")
        }
    }
}