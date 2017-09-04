package com.chadbingham.loyautils.misc

interface Selector<T, out R> {
    fun select(t: T): R
}