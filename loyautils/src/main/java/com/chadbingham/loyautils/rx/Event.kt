package com.chadbingham.loyautils.rx

typealias EmptyEvent<T> = Event.Empty<T>
typealias OriginalEvent<T> = Event.Original<T>
typealias AddedEvent<T> = Event.Added<T>
typealias RemovedEvent<T> = Event.Removed<T>
typealias ChangedEvent<T> = Event.Changed<T>
typealias CancelledEvent<T> = Event.Canceled<T>

sealed class Event<out T>(val value: T?) {

    val hasValue: Boolean
        get() = value != null

    class Empty<out T> : Neutral<T>(null)

    open class Positive<out T>(value: T) : Event<T>(value)

    open class Negative<out T>(value: T?) : Event<T>(value)

    open class Neutral<out T>(value: T?) : Event<T>(value)

    /* For events that were added before the observer subscribed. */
    class Original<out T>(value: T) : Positive<T>(value)

    class Added<out T>(value: T) : Positive<T>(value)

    class Changed<out T>(value: T) : Positive<T>(value)

    class Removed<out T>(value: T? = null) : Negative<T>(value)

    class Canceled<out T>(val error: Throwable? = null) : Event<T>(null)
}