package com.example.library

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import android.view.View


class Registration : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        initViews()
    }
    //initialize all UI Elements
    private fun initViews(){
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        registerButton = findViewById(R.id.register)
    }
    //This method is called when the register button is pressed
    fun registerNewUser(view: View){
        var uname = username.getText().toString()
        var pwd = password.getText().toString()
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "local_db"
        ).allowMainThreadQueries().build()
        val userDao = db.userDao()
        val user = User(0 ,uname, pwd)
        userDao.insertAll(user)
        finish()
    }
}
