package com.example.library

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.library.favorites.FavoritesActivity
import com.example.library.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener{

    private lateinit var navigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        navigationView = findViewById(R.id.bottom_navigation)
        navigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()
        updateNavigationBarState()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0,0)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigationView.postDelayed({
            when(item.itemId) {
                R.id.account -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.cameraView -> startActivity(Intent(this, MainActivity::class.java))
                R.id.favoriteList -> startActivity(Intent(this, FavoritesActivity::class.java))
            }
            finish()
        }, 300)
        return true
    }

    private fun updateNavigationBarState(){
        val actionId = getNavigationMenuItemId()
        selectBottomNavigationBarItem(actionId)
    }

    private fun selectBottomNavigationBarItem(itemId: Int){
        val menuItem = navigationView.menu.findItem(itemId)
        menuItem.isChecked = true
    }

    abstract fun getLayoutId(): Int
    //this is to return which layout(activity) needs to display when clicked on tabs

    abstract fun getNavigationMenuItemId(): Int
    //Which menu item selected and change the state of that menu item
}