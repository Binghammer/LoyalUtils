package com.chadbingham.loyautils.rx

import io.reactivex.Single

@Suppress("unused", "MemberVisibilityCanPrivate")
abstract class RxRunner<T> {

    var value: T? = null
        protected set

    var complete = false
        private set

    var error: Throwable? = null
        protected set

    val hasValue: Boolean
        get() = complete && value != null

    val hasError: Boolean
        get() = complete && error != null

    val isSuccessful: Boolean
        get() = complete && error == null

    fun run(): Single<RxRunner<T>> {
        check(!complete, { "RxRunnable has already ran" })
        return runner()
                .doFinally { complete = true }
                .doOnError { error = it }
                .doOnSuccess {
                    complete = true
                    value = it
                }
                .map { this }
    }

    protected abstract fun runner(): Single<T>
}