package com.chadbingham.loyautils.rx

import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ScheduleFlowable {
    fun <T> io(): FlowableTransformer<T, T> {
        return FlowableTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> computation(): FlowableTransformer<T, T> {
        return FlowableTransformer { o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()) }
    }
}
