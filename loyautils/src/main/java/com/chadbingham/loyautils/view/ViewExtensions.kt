package com.chadbingham.loyautils.view

import android.graphics.Rect
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
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
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

fun View.setPadding(padding: Int) {
    setPadding(padding, padding, padding, padding)
}

fun View.setMargins(margin: Int) {
    val mlp = ViewGroup.MarginLayoutParams(layoutParams)
    mlp.margins(margin)
    layoutParams = mlp
}

fun ViewGroup.MarginLayoutParams.margins(margin: Int) {
    setMargins(margin, margin, margin, margin)
}

fun View.layout(rect: Rect) {
    layout(rect.left, rect.top, rect.right, rect.bottom)
}