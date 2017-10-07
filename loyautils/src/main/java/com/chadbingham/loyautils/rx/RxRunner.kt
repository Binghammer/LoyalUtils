package com.chadbingham.loyautils.rx

import io.reactivex.Completable

abstract class RxRunner {

    var complete = false

    var error: Throwable? = null

    fun hasError(): Boolean = error != null

    fun isSuccessfull(): Boolean = complete && error == null

    abstract fun run(): Completable
}