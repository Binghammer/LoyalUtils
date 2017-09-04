package com.chadbingham.loyautils.rx

import com.chadbingham.loyautils.fire.Mapper

sealed class Event<T>(val value: T?) {

    abstract fun <R> map(mapper: Mapper<T, R>): Event<R>

    class Empty<T> : Event<T>(null) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Empty()
        }
    }

    class Added<T>(value: T) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Added(mapper.map(value!!))
        }
    }

    class Changed<T>(value: T) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Changed(mapper.map(value!!))
        }
    }

    class Removed<T>(value: T) : Event<T>(value) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Removed(mapper.map(value!!))
        }
    }

    class Canceled<T>(val error: Exception? = null) : Event<T>(null) {
        override fun <R> map(mapper: Mapper<T, R>): Event<R> {
            return Canceled()
        }
    }

}