package com.chadbingham.loyautils.misc

interface Selector<in T, out R> {
    fun select(t: T): R
}