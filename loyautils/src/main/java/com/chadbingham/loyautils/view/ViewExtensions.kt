@file:Suppress("unused")

package com.chadbingham.loyautils.view

import android.content.Context
import android.graphics.Rect
import android.support.design.widget.TextInputLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import io.reactivex.Observable

fun View.locationOnScreen(): IntArray {
    val arr = IntArray(2)
    getLocationOnScreen(arr)
    return arr
}

fun View.xLocationOnScreen(): Int = locationOnScreen()[0]

fun View.yLocationOnScreen(): Int = locationOnScreen()[1]

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

fun TextView.getString(): String = text?.toString() ?: ""

fun TextInputLayout.getString(): String = editText?.getString() ?: ""

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

val Context.inputMethodManager: InputMethodManager
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

fun View.hideSoftKeyboard() {
    context.inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun EditText.showSoftKeyboard() {
    if (requestFocus()) context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.toggleSoftKeyboard() {
    if (requestFocus()) context.inputMethodManager.toggleSoftInput(0, 0)
}

fun RecyclerView.ViewHolder.getString(id: Int): String {
    return itemView.context.getString(id)
}

fun RecyclerView.ViewHolder.getString(id: Int, vararg values: Any): String {
    return itemView.context.getString(id, values)
}