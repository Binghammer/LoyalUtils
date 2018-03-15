@file:Suppress("unused", "UNCHECKED_CAST")

package com.chadbingham.loyautils

import android.content.Context
import android.content.SharedPreferences
import java.util.*
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SharedPrefs<T>(
        private val defaultValue: T,
        private val namespace: String = "_preferences",
        private val privacy: Int = Context.MODE_PRIVATE,
        private val onChange: (() -> Unit)? = null) : ReadWriteProperty<Any, T> {

    companion object {
        private var context: Context by Delegates.notNull()
        private val prefs_map: HashMap<String, SharedPreferences> = HashMap()

        fun setPrefContext(context: Context) {
            this.context = context
        }

        fun clear(namespace: String? = null) {
            if (namespace != null) {
                if (prefs_map.containsKey(namespace)) {
                    prefs_map[namespace]?.edit()?.clear()?.apply()
                }
            } else {
                prefs_map.forEach {
                    it.value.edit().clear().apply()
                }
            }
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val name = property.name

        if (!prefs_map.containsKey(namespace)) {
            prefs_map[namespace] = context.getSharedPreferences("${context.packageName}.$namespace", privacy)
        }

        val prefs = prefs_map[namespace]!!
        return when (defaultValue) {
            is Boolean -> prefs.getBoolean(name, defaultValue) as T
            is Float -> prefs.getFloat(name, defaultValue) as T
            is Int -> prefs.getInt(name, defaultValue) as T
            is Long -> prefs.getLong(name, defaultValue) as T
            is String -> prefs.getString(name, defaultValue) as T
            is Set<*> -> prefs.getStringSet(name, defaultValue as Set<String>) as T
            else -> throw UnsupportedOperationException("Unsupported preference type ${property.javaClass} on property $name")
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val name = property.name
        if (!prefs_map.containsKey(namespace)) {
            prefs_map[namespace] = context.getSharedPreferences("${context.packageName}.$namespace", privacy)
        }

        prefs_map[namespace]!!.edit().let {
            when (defaultValue) {
                is Boolean -> it.putBoolean(name, value as Boolean)
                is Float -> it.putFloat(name, value as Float)
                is Int -> it.putInt(name, value as Int)
                is Long -> it.putLong(name, value as Long)
                is String -> it.putString(name, value as String)
                is Set<*> -> it.putStringSet(name, value as Set<String>)
                else -> throw UnsupportedOperationException("Unsupported preference type ${property.javaClass} on property $name")
            }

            onChange?.invoke()
            it.apply()
        }
    }
}