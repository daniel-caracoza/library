package com.example.library

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException


class LoginActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var password: EditText
    //private lateinit var loginButton: Button
    private lateinit var sign_in_button: SignInButton
    //private lateinit var sign_up_button: Button
    private var RC_SIGN_IN = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initViews()
        //integrate google sign in
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        sign_in_button.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
        var loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener {
            userLogin()
        }
        var sign_up_button = findViewById<Button>(R.id.signup)
        sign_up_button.setOnClickListener{
            val intent = Intent(this, Registration::class.java)
            startActivity(intent)
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

            // Signed in successfully, show authenticated UI.
            //updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            //updateUI(null)
        }

    }

    //initialize UI elements
    private fun initViews(){
        username = findViewById(R.id.username)
        password = findViewById (R.id.password)
        //loginButton = findViewById(R.id.login)
        sign_in_button = findViewById(R.id.sign_in_button)
        //sign_up_button = findViewById(R.id.signup)
    }

    fun userLogin(){
        var uname = username.text.toString()
        var pwd = password.text.toString()
        var db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "local_db"
        ).allowMainThreadQueries().build()
        var userDao = db.userDao()
        if(userDao.findUser(uname, pwd) != null){
            print(userDao.findUser(uname, pwd))
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    //when the app has stopped and restarted, check if someone is logged in with google account
    override fun onStart() {
        super.onStart()
        var account:GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}

