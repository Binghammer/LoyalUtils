package com.chadbingham.loyautils.rx

import io.reactivex.MaybeTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ScheduleMaybe {

    fun <T> io(): MaybeTransformer<T, T> {
        return MaybeTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> computation(): MaybeTransformer<T, T> {
        return MaybeTransformer { o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()) }
    }
}
