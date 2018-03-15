package com.chadbingham.loyautils.rx

import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object RxUtil {
    fun <T> newTimedReplayProcessor(seconds: Long): ReplayProcessor<T> {
        return ReplayProcessor.createWithTime(seconds, TimeUnit.SECONDS, Schedulers.computation())
    }
}