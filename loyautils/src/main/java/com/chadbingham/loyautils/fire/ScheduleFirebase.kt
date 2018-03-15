package com.chadbingham.loyautils.fire

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

object ScheduleFirebase {

    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    fun <T> flowable(): FlowableTransformer<T, T> {
        return FlowableTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> observable(): ObservableTransformer<T, T> {
        return ObservableTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> single(): SingleTransformer<T, T> {
        return SingleTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun <T> maybe(): MaybeTransformer<T, T> {
        return MaybeTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun completable(): CompletableTransformer {
        return CompletableTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }
}
