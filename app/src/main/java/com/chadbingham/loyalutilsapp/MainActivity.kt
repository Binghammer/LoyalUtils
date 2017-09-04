package com.chadbingham.loyalutilsapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.chadbingham.loyautils.view.DynamicTextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tv: DynamicTextView = findViewById(R.id.tv)
    }
}
