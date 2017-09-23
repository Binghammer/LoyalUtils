package com.chadbingham.loyautils.rx

import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ScheduleSingle {

    fun <T> io(): SingleTransformer<T, T> {
        return SingleTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> computation(): SingleTransformer<T, T> {
        return SingleTransformer { o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> main(): SingleTransformer<T, T> {
        return SingleTransformer { o -> o.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread()) }
    }

}
