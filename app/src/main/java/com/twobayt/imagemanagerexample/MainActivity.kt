package com.twobayt.imagemanagerexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private var fragment:HomeFragment ?  = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            fragment = HomeFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment!!)
                .commit()
        }
    }

}