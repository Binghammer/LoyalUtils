package com.chadbingham.loyautils.misc

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyDelegate<R, T>(private var t: T) : ReadWriteProperty<R, T> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return t
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        t = value
    }
}