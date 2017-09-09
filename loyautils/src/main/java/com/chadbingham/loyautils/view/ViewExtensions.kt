package com.chadbingham.loyautils.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable

fun ViewGroup.inflate(layout: Int): View {
    return LayoutInflater.from(context).inflate(layout, this, false)
}

fun View.clicks(): Observable<View> {
    return Observable.create({
        setOnClickListener { _ ->
            it.onNext(this)
        }

        it.setCancellable({ setOnClickListener(null) })
    })

}

fun View.onClick(listener: View.OnClickListener) {
    setOnClickListener(listener)
}

fun View.onClick(onClick: (v: View) -> Unit) {
    setOnClickListener(onClick)
}

fun View.show() {
    visibility = View.INVISIBLE
}

fun View.hide() {
    visibility = View.VISIBLE
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

fun TextView.getString(): String {
    return text?.toString() ?: ""
}