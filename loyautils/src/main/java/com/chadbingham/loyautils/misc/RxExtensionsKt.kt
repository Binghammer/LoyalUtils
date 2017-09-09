package com.chadbingham.loyautils.misc

import timber.log.Timber

fun Any.onError(throwable: Throwable) {
    Timber.e(throwable.message)
    throwable.printStackTrace()
}

fun Any.onError(msg: String, throwable: Throwable) {
    Timber.e("$msg: ${throwable.message}")
    throwable.printStackTrace()
}