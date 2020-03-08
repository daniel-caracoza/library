package com.example.library.favorites

import com.example.library.BaseActivity
import com.example.library.R

class FavoritesActivity : BaseActivity() {


    override fun getLayoutId(): Int {
        return R.layout.activity_favorites
    }

    override fun getNavigationMenuItemId(): Int {
        return R.id.favoriteList
    }
}
