package com.example.library.loginFlow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.library.utils.HashUtils
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.database.UserDao


class Registration : AppCompatActivity(), View.OnClickListener{
    private lateinit var fullname: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var account_exists: TextView
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        initViews()
        initListeners()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "local_db"
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }
    //initialize all UI Elements
    private fun initViews(){
        fullname = findViewById(R.id.fullname)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        registerButton = findViewById(R.id.register)
        account_exists = findViewById<TextView>(R.id.account_exists)
    }
    private fun initListeners() {
        registerButton.setOnClickListener(this)
        account_exists.setOnClickListener(this)
    }

    override fun onClick(v:View?){
        when(v?.id){
            R.id.register -> {registerNewUser()}
            R.id.account_exists -> { finish() }
        }
    }
    //This method is called when the register button is pressed
    private fun registerNewUser(){
        val fname = fullname.text.toString()
        val uname = username.text.toString()
        val pwd = HashUtils.sha1(password.text.toString())
        if(!(!validatePassword() && !validateUsername(uname) && !validateName())) {
            if (userDao.findUser(uname, pwd) == null) {
                val user =
                    User(0, fname, uname, pwd)
                userDao.insertAll(user)
                Toast.makeText(this, "Account created.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    private fun validateName():Boolean {
        //if full name is not empty
        return when (fullname.text.isEmpty()) {
            true -> {
                fullname.error = "Field can't be empty"
                false
            }
            false -> {
                fullname.error = null
                true
            }
        }
    }

    private fun validateUsername(uname:String): Boolean {
        return when (username.text.isEmpty()) {
            true -> {
                username.error = "Field can't be empty."
                false
            }
            false -> {
                if(!Patterns.EMAIL_ADDRESS.matcher(uname).matches()){
                    username.error = "Please enter a valid email address."
                    return false
                }
                else if(userDao.findusername(uname) != null){
                    username.error = "Username is already in use."
                    return false
                }
                username.error = null
                true
            }
        }
    }

    private fun validatePassword():Boolean {
        return when (password.text.isEmpty()) {
            true -> {
                password.error = "Field can't be empty."
                false
            }
            false -> {
                password.error = null
                true
            }
        }
    }
}
