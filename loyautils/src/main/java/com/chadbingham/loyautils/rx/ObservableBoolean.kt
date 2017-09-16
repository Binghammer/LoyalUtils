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

    fun whenTrueSkipFirst(runnable: () -> Any?): Disposable {
        return subject.skip(1).filter({ !it }).subscribe({ runnable() })
    }

    fun whenFalseSkipFirst(runnable: () -> Any?): Disposable {
        return subject.skip(1).filter({ it }).subscribe({ runnable() })
    }

    fun whenTrue(runnable: () -> Any?): Disposable {
        return subject.filter({ it }).subscribe({ runnable() })
    }

    fun whenFalse(runnable: () -> Any?): Disposable {
        return subject.filter({ !it }).subscribe({ runnable() })
    }

}