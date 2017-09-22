package com.chadbingham.loyautils.rx

import com.chadbingham.loyautils.fire.Mapper

sealed class Event<out T>(val value: T?) {

    abstract fun <R> map(mapper: Mapper<T, R>): Event<R>

    fun hasValue(): Boolean {
        return value != null
    }

    class Empty<out T> : Event<T>(null) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Empty()
        }
    }

    class Added<out T>(value: T) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Added(mapper.map(value!!)!!)
        }
    }

    class Changed<out T>(value: T) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Changed(mapper.map(value!!)!!)
        }
    }

    class Removed<out T>(value: T? = null) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Removed(mapper.map(value!!)!!)
        }
    }

    class Canceled<out T>(val error: Exception? = null) : Event<T>(null) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Canceled()
        }
    }

}