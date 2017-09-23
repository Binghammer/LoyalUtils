package com.chadbingham.loyautils.misc

fun <T> List<T>.copy(): List<T> {
    val copy = arrayListOf<T>()
    copy.addAll(this)
    return copy.toList()
}

fun <T> List<T>.mutableCopy(): MutableList<T> {
    val copy = mutableListOf<T>()
    copy.addAll(this)
    return copy
}

fun <T> ArrayList<T>.copy(): ArrayList<T> {
    val copy = arrayListOf<T>()
    copy.addAll(this)
    return copy
}

fun <T> ArrayList<T>.mutableCopy(): MutableList<T> {
    val copy = mutableListOf<T>()
    copy.addAll(this)
    return copy
}