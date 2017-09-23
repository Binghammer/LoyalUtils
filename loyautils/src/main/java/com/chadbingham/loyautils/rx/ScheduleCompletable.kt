package com.chadbingham.loyautils.rx

import io.reactivex.CompletableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ScheduleCompletable {

    fun io(): CompletableTransformer {
        return CompletableTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun computation(): CompletableTransformer {
        return CompletableTransformer { o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun computationOnly(): CompletableTransformer {
        return CompletableTransformer { o -> o.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation()) }
    }

    fun main(): CompletableTransformer {
        return CompletableTransformer { o ->
            o.subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
        }
    }

}
