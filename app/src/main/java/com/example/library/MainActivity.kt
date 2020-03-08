package com.example.library

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem


class MainActivity : BaseActivity() {

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(getLayoutId())
//        setSupportActionBar(findViewById(R.id.main_toolbar))
//    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun getNavigationMenuItemId(): Int {
        return R.id.cameraView
    }


//    //inflates the menu for toggling views.
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        val inflater: MenuInflater = menuInflater
//        inflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//
//    //toggles the menu items with checks/no-check depending on state
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.toggle_price, R.id.toggle_reviews, R.id.toggle_recommendations-> {
//                item.isChecked = !item.isChecked
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }


}
