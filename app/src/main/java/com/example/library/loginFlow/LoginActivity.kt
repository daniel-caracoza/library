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
import com.example.library.utils.HashUtils
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.UserDao
import com.example.library.home.SharedCameraActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var sign_in_button: SignInButton
    private lateinit var sign_up_button: Button
    private var RC_SIGN_IN = 0
    private lateinit var userDao: UserDao
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById(R.id.login_toolbar))
        initViews()
        initListeners()
        val application = requireNotNull(this).application
        userDao = AppDatabase.getInstance(application).userDao
        sharedPreferences = getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("loggedIn", false).apply()
    }
    override fun onClick(v:View?){
        when(v?.id){
            R.id.login -> { userLogin() }
            R.id.signup -> { startRegistration() }
            R.id.sign_in_button -> googleSignIn()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            startActivity(Intent(this, SharedCameraActivity::class.java))
            finish()
            // Signed in successfully, show authenticated UI.
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
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
        val foundUser = userDao.findUser(uname, pwd)
        if(foundUser != null){
            //logged in user id
            sharedPreferences.edit().putInt("userid", foundUser.uid).apply()
            sharedPreferences.edit().putBoolean("loggedIn", true).apply()
            val intent = Intent(this, SharedCameraActivity::class.java)
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
            startActivity(Intent(this, SharedCameraActivity::class.java))
            finish()
        }
        super.onStart()
    }
    /*
     integrate google sign in
     Configure sign-in to request the user's ID, email address, and basic
     profile. ID and basic profile are included in DEFAULT_SIGN_IN.
     Build a GoogleSignInClient with the options specified by gso.
     */
    private fun googleSignIn(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

}

