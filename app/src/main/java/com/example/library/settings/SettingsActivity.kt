package com.example.library.settings

import com.example.library.BaseActivity
import com.example.library.R

class SettingsActivity : BaseActivity() {


    override fun getNavigationMenuItemId(): Int {
        return R.id.account
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_settings
    }
}
