package com.chadbingham.loyautils.misc

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

fun runOnMain(block: () -> Unit) {
    Single.just(Unit)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { _, _ -> block() }
}

inline fun whenNull(block: () -> Unit) {
    block()
}