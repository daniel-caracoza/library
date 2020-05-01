package com.example.library.loginFlow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.library.MainActivity
import com.example.library.utils.HashUtils
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.database.UserDao
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.*


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var sign_in_button: SignInButton
    private lateinit var sign_up_button: Button
    private var RC_SIGN_IN = 1
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var userDao: UserDao
    private lateinit var sharedPreferences: SharedPreferences
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById(R.id.login_toolbar))
        initViews()
        initListeners()
        val application = requireNotNull(this).application
        configureGoogleSignIn()
        userDao = AppDatabase.getInstance(application).userDao
        sharedPreferences = getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("loggedIn", false).apply()
    }
    override fun onClick(v:View?){
            when (v?.id) {
                R.id.login -> {
                    userLogin()
                }
                R.id.signup -> {
                    startRegistration()
                }
                R.id.sign_in_button -> googleSignIn()
            }
    }

    private fun configureGoogleSignIn(){
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1:816570324968:android:40fce80fa9d53a72baf65d")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    private fun googleSignIn(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            //use this account to authenticate firebase!
            val account = completedTask.getResult(ApiException::class.java)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: ApiException) {
            Toast.makeText(this, "Failed Sign In", Toast.LENGTH_LONG).show()
        }

    }

    //initialize UI elements
    private fun initViews(){
        username = findViewById(R.id.username)
        password = findViewById (R.id.password)
        loginButton = findViewById<Button>(R.id.login)
        sign_in_button = findViewById(R.id.sign_in_button)
        sign_up_button = findViewById<Button>(R.id.signup)
    }
    private fun initListeners(){
        sign_up_button.setOnClickListener(this)
        loginButton.setOnClickListener(this)
        sign_in_button.setOnClickListener(this)
    }
    private fun userLogin(){
        val uname = username.text.toString()
        val pwd = HashUtils.sha1(password.text.toString())
        var foundUser:User? = null
        uiScope.launch { withContext(Dispatchers.IO){foundUser = userDao.findUser(uname, pwd) }}
        if(foundUser != null){
            //logged in user id
            sharedPreferences.edit().putInt("userid", foundUser!!.uid).apply()
            sharedPreferences.edit().putBoolean("loggedIn", true).apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun startRegistration(){
        val intent = Intent(this, Registration::class.java)
        startActivity(intent)
    }

    //when the app has stopped and restarted, check if someone is logged in with google account
    override fun onStart() {
        val account:GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        //check whether logged in with google account or library++ account
        if (account != null || sharedPreferences.getBoolean("loggedIn", false)){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        super.onStart()
    }

}

