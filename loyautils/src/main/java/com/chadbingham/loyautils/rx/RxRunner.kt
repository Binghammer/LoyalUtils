package com.chadbingham.loyautils.rx

import io.reactivex.Completable

@Suppress("unused", "MemberVisibilityCanPrivate")
abstract class RxRunner {

    var complete = false

    var error: Throwable? = null

    fun hasError(): Boolean = error != null

    fun isSuccessful(): Boolean = complete && error == null

    abstract fun run(): Completable
}