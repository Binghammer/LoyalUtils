package com.chadbingham.loyautils.view

import android.view.View

fun View.clicks() : Observable{

}

fun View.onClick(listener: View.OnClickListener) {
    setOnClickListener(listener)
}

fun View.onClick(onClick: (v: View) -> Unit) {
    setOnClickListener(onClick)
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}