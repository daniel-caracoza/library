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
import kotlinx.coroutines.*


class Registration : AppCompatActivity(), View.OnClickListener {
    private lateinit var fullname: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var registerButton: Button
    private lateinit var account_exists: TextView
    private lateinit var userDao: UserDao
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        initViews()
        initListeners()
        val application = requireNotNull(this).application
        userDao = AppDatabase.getInstance(application).userDao
    }

    //initialize all UI Elements
    private fun initViews() {
        fullname = findViewById(R.id.fullname)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        registerButton = findViewById(R.id.register)
        account_exists = findViewById(R.id.account_exists)
    }

    private fun initListeners() {
        registerButton.setOnClickListener(this)
        account_exists.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.register -> {
                registerNewUser()
            }
            R.id.account_exists -> {
                finish()
            }
        }
    }

    //This method is called when the register button is pressed
    private fun registerNewUser() {
        val fname = fullname.text.toString()
        val uname = username.text.toString()
        val pwd = HashUtils.sha1(password.text.toString())
        if (validatePasswordView() && validateUsernameView(uname) && validateNameView()) {
            val user = User(0, fname, uname, pwd)
            uiScope.launch {
                withContext(Dispatchers.IO) {
                    userDao.insertAll(user)
                }
            }
            Toast.makeText(this, "Account created.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun validateNameView(): Boolean =
        when (fullname.text.isNotEmpty()) {
            true -> true
            false -> {
                fullname.error = "Field can't be empty"
                false
            }
        }

    private fun validateUsernameView(uname: String): Boolean =
        when(username.text.isNotEmpty() || !Patterns.EMAIL_ADDRESS.matcher(uname).matches()) {
            false -> {
                username.error = "Username invalid"
                false
            }
            true -> {
                var boolean: Boolean = true
                var foundName: String? = null
                uiScope.launch { withContext(Dispatchers.IO){foundName = userDao.findusername(uname)?.userName} }
                if(uname == foundName){
                    username.error == "Username already exists!"
                    boolean = false
                }
                boolean
            }
        }


    private fun validatePasswordView():Boolean {
        val test = password.text.isNotEmpty()
        return when (test) {
            true -> true
            false -> {
                password.error = "Password can't be empty"
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
