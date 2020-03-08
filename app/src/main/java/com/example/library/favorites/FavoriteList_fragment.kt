package com.example.library.favorites

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.databinding.FragmentFavoriteListFragmentBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class FavoriteList_fragment : Fragment() {

    private var userId:Int = 0
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentFavoriteListFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_favorite_list_fragment, container, false)

        val application = requireNotNull(this.activity).application
        sharedPreferences = application.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
        val dataSource = AppDatabase.getInstance(application).favoriteDao

        val viewModelFactory = FavoritesViewModelFactory(userId, dataSource, application)

        val favoritesViewModel = ViewModelProviders.of(
            this, viewModelFactory).get(FavoritesViewModel::class.java)
        binding.favoriteViewModel = favoritesViewModel
        binding.lifecycleOwner = this

        val adapter = FavoriteAdapter()

        binding.favoriteList.adapter = adapter

        favoritesViewModel.favorites.observe(viewLifecycleOwner, Observer {
            it?.let{
                adapter.submitList(it)
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrieveSignIn()
    }

    private fun retrieveSignIn(){

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)
        val uid = sharedPreferences.getInt("userid", 0)

        userId = when(account == null){
            true -> uid
            false -> {
                val gid = account.id!!
                val truncate = gid.substring(0, gid.length - 12)
                truncate.toInt()
            }
        }
    }
}

