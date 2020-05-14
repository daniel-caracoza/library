package com.example.library.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.database.UserDao
import com.example.library.loginFlow.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import kotlinx.coroutines.*


class Settings_fragment : Fragment(), View.OnClickListener {

    lateinit var profile_image:ImageView
    lateinit var acct_name:TextView
    lateinit var acct_email:TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userDao: UserDao
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var application: Context
    private var user: User? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        application = requireNotNull(this.activity).application
        sharedPreferences = application.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
        userDao = AppDatabase.getInstance(application).userDao
        return inflater.inflate(R.layout.fragment_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profile_image = view.findViewById<ImageView>(R.id.uri)
        acct_name = view.findViewById<TextView>(R.id.acct_name)
        acct_email = view.findViewById(R.id.email)
        view.findViewById<Button>(R.id.logout).setOnClickListener(this)
        googleAccount()
    }


    override fun onClick(v: View?) {
            when (v?.id) {
                R.id.logout -> {
                    userLogout()
                    requireActivity().finish()
                }
            }
    }

    private fun userLogout(){
        sharedPreferences.edit().putBoolean("loggedIn", false).apply()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(requireActivity().applicationContext, gso)
        mGoogleSignInClient.signOut().addOnCompleteListener(OnCompleteListener<Void> {
            startActivity(Intent(activity, LoginActivity::class.java))
        })

    }

    private fun googleAccount() {
            val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)

            if (account != null) {
                acct_name.text = account.displayName
                acct_email.text = account.email
                Glide.with(application).load(account.photoUrl).apply(RequestOptions.circleCropTransform())
                    .into(profile_image)
            } else {
                val libraryUserId = sharedPreferences.getLong("userid", 0)
                user = runBlocking {displayUser(libraryUserId)}
                acct_name.text = user?.fullname
                acct_email.text = user?.userName
                profile_image.setImageResource(R.drawable.ic_account2)
            }
    }

    private suspend fun displayUser(userid: Long): User? {
        return withContext(Dispatchers.IO) {
            userDao.findUserById(userid)
        }
    }
}
