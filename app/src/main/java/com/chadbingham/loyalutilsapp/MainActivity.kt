package com.chadbingham.loyalutilsapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val factory = ActivityPresenterFactory(this, R.id.container)

//        val presenter = factory.presenterTest

//        val test: InteractorTest = Interactors(this, R.id.container).interactorTest
    }
}
