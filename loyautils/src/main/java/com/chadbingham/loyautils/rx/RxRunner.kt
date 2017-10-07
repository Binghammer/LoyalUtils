package com.chadbingham.loyautils.rx

import io.reactivex.Completable
import io.reactivex.Single

@Suppress("unused", "MemberVisibilityCanPrivate")
abstract class RxRunner {

    var complete = false
        private set

    var error: Throwable? = null

    fun hasError(): Boolean = error != null

    fun isSuccessful(): Boolean = complete && error == null

    fun run(): Single<RxRunner> {
        return completable()
                .doFinally { complete = true }
                .doOnError { this.error = it }
                .andThen(Single.just(this))
    }

    abstract fun completable(): Completable
}