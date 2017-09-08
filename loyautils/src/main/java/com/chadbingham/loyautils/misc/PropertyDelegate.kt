package com.chadbingham.loyautils.misc

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> T.delegate(): PropertyDelegate<Any, T> {
    return PropertyDelegate(this)
}

class PropertyDelegate<R, T>(private var t: T) : ReadWriteProperty<R, T> {

    operator fun provideDelegate(thisRef: R, property: KProperty<*>) {

    }

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return t
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        t = value
    }
}