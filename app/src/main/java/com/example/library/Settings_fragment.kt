package com.example.library

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.OnCompleteListener



class Settings_fragment : Fragment(), View.OnClickListener {
    lateinit var logoutbutton:Button
    lateinit var profile_image:ImageView
    lateinit var acct_name:TextView
    lateinit var acct_email:TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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
        when(v?.id){
            R.id.logout -> {userLogout()}
        }
    }

    private fun userLogout(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(activity!!.applicationContext, gso)
        mGoogleSignInClient.signOut().addOnCompleteListener(OnCompleteListener<Void> {
            startActivity(Intent(activity, LoginActivity::class.java))
        })

    }

    private fun googleAccount(){
        var account:GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)
        acct_name.text = account?.displayName
        acct_email.text = account?.email
        Glide.with(this).load(account?.photoUrl).apply(RequestOptions.circleCropTransform()).into(profile_image)
    }
}
