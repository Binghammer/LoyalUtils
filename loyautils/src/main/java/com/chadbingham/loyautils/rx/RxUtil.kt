package com.chadbingham.loyautils.rx

import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.SingleSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

object RxUtil {

    fun isSubjectAlive(subject: Subject<*>?): Boolean {
        return subject != null && !subject.hasComplete() && !subject.hasThrowable()
    }

    fun isSubjectAlive(subject: SingleSubject<*>?): Boolean {
        return subject != null && !subject.hasThrowable()
    }

    fun isSubjectDead(subject: Subject<*>?): Boolean {
        return subject == null || subject.hasComplete() || subject.hasThrowable()
    }

    fun isAliveAndHasObservers(subject: Subject<*>?): Boolean {
        return isSubjectAlive(subject) && subject!!.hasObservers()
    }

    fun disposeStrict(disposables: CompositeDisposable?) {
        disposables?.dispose()
    }

    fun dispose(disposables: CompositeDisposable?) {
        disposables?.clear()
    }

    fun dispose(vararg disposables: Disposable) {
        disposables
                .filterNot { it.isDisposed }
                .forEach { it.dispose() }
    }

    fun <T> newFiveMinuteReplayProcessor(): ReplayProcessor<T> {
        return ReplayProcessor.createWithTime(5, TimeUnit.MINUTES, Schedulers.computation())
    }

    fun isDisposed(d: Disposable?): Boolean {
        return d == null || d.isDisposed
    }

    fun notDisposed(d: Disposable?): Boolean {
        return d != null && !d.isDisposed
    }

    fun <T> composeToSet(): ObservableTransformer<T, Set<T>> {
        return ObservableTransformer { upstream -> upstream.collectInto(mutableSetOf<T>(), { obj, e -> obj.add(e) }).toObservable().map { it.toSet() } }
    }
}