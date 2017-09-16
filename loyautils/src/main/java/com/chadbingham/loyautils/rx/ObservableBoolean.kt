package com.chadbingham.loyautils.rx

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class ObservableBoolean(initial: Boolean = false) {

    private val subject: PublishSubject<Boolean> = PublishSubject.create()

    init {
        subject.onNext(initial)
    }

    fun set(value: Boolean) {
        subject.onNext(value)
    }

    fun whenTrueSkipFirst(runnable: Runnable): Disposable {
        return subject.skip(1).filter({ !it }).subscribe({ runnable.run() })
    }

    fun whenFalseSkipFirst(runnable: Runnable): Disposable {
        return subject.skip(1).filter({ it }).subscribe({ runnable.run() })
    }

    fun whenTrue(runnable: Runnable): Disposable {
        return subject.filter({ it }).subscribe({ runnable.run() })
    }

    fun whenFalse(runnable: Runnable): Disposable {
        return subject.filter({ !it }).subscribe({ runnable.run() })
    }

}