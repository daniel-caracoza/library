package com.example.library

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import android.view.View
import android.widget.TextView


class Registration : AppCompatActivity(), View.OnClickListener{

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var account_exists: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        initViews()
        initListeners()
    }
    //initialize all UI Elements
    private fun initViews(){
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        registerButton = findViewById(R.id.register)
        account_exists = findViewById<TextView>(R.id.account_exists)
    }
    private fun initListeners(){
        registerButton.setOnClickListener(this)
        account_exists.setOnClickListener(this)
    }

    override fun onClick(v:View?){
        when(v?.id){
            R.id.register -> { registerNewUser()}
            R.id.account_exists -> { finish() }
        }
    }
    //This method is called when the register button is pressed
    private fun registerNewUser(){
        var uname = username.text.toString()
        var pwd = password.text.toString()
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
