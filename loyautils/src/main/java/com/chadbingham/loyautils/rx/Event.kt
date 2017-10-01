@file:Suppress("unused")

package com.chadbingham.loyautils.rx

typealias EmptyEvent<T> = Event.Empty<T>
typealias JustEvent<T> = Event.Just<T>
typealias AddedEvent<T> = Event.Added<T>
typealias RemovedEvent<T> = Event.Removed<T>
typealias ChangedEvent<T> = Event.Changed<T>
typealias CancelledEvent<T> = Event.Canceled<T>
typealias ErrorEvent<T> = Event.Error<T>

sealed class Event<out T>(val value: T?) {

    val hasValue: Boolean
        get() = value != null

    val isPositive: Boolean
        get() = this is Positive

    val isNegative: Boolean
        get() = this is Negative

    open class Positive<out T>(value: T) : Event<T>(value)

    open class Negative<out T>(value: T?) : Event<T>(value)

    class Empty<out T> : Event<T>(null)

    /* For events that were added before the observer subscribed. */
    class Just<out T>(value: T) : Positive<T>(value)

    class Added<out T>(value: T) : Positive<T>(value)

    class Changed<out T>(value: T) : Positive<T>(value)

    class Removed<out T>(value: T? = null) : Negative<T>(value)

    class Canceled<out T>(val message: String? = null) : Negative<T>(null)

    class Error<out T>(val error: Throwable? = null) : Negative<T>(null)
}