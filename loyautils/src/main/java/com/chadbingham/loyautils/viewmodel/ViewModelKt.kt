package com.chadbingham.loyautils.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.graphics.drawable.Drawable
import android.support.annotation.CallSuper
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import org.koin.standalone.KoinComponent

open class ViewModelRx : ViewModel(), KoinComponent {

    private val disposables: CompositeDisposable = CompositeDisposable()

    protected fun <T> Observable<T>.bindToViewModel(): Observable<T> = compose(Transform<T>())
    protected fun <T> Single<T>.bindToViewModel(): Single<T> = compose(Transform<T>())
    protected fun <T> Maybe<T>.bindToViewModel(): Maybe<T> = compose(Transform<T>())

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    private inner class Transform<T> : ObservableTransformer<T, T>, SingleTransformer<T, T>, MaybeTransformer<T, T> {
        override fun apply(up: Observable<T>): ObservableSource<T> = up.doOnSubscribe { disposables.add(it) }
        override fun apply(up: Single<T>): SingleSource<T> = up.doOnSubscribe { disposables.add(it) }
        override fun apply(up: Maybe<T>): MaybeSource<T> = up.doOnSubscribe { disposables.add(it) }
    }
}

open class AndroidViewModelRx(context: Application) : AndroidViewModel(context) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    protected val context
        get() = getApplication<Application>()

    protected fun <T> Observable<T>.bindToViewModel(): Observable<T> = compose(Transform<T>())
    protected fun <T> Single<T>.bindToViewModel(): Single<T> = compose(Transform<T>())
    protected fun <T> Maybe<T>.bindToViewModel(): Maybe<T> = compose(Transform<T>())

    protected fun string(@StringRes id: Int): String = context.getString(id)
    protected fun string(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
    protected fun drawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(context, id)!!
    protected fun color(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    private inner class Transform<T> : ObservableTransformer<T, T>, SingleTransformer<T, T>, MaybeTransformer<T, T> {
        override fun apply(up: Observable<T>): ObservableSource<T> = up.doOnSubscribe { disposables.add(it) }
        override fun apply(up: Single<T>): SingleSource<T> = up.doOnSubscribe { disposables.add(it) }
        override fun apply(up: Maybe<T>): MaybeSource<T> = up.doOnSubscribe { disposables.add(it) }
    }
}