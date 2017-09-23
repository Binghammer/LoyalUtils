package com.chadbingham.loyautils.rx

import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ScheduleObservable {

    fun <T> io(): ObservableTransformer<T, T> {
        return ObservableTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> computation(): ObservableTransformer<T, T> {
        return ObservableTransformer { o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> main(): ObservableTransformer<T, T> {
        return ObservableTransformer { o ->
            o.subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
        }
    }
}