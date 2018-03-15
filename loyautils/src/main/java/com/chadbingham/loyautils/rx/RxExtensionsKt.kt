package com.chadbingham.loyautils.rx

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class ScheduleTransformers {
    protected val main: Scheduler
        get() = AndroidSchedulers.mainThread()!!

    protected val network: Scheduler
        get() = Schedulers.io()

    protected val computation: Scheduler
        get() = Schedulers.computation()
}

object ScheduleCompletable : ScheduleTransformers() {
    fun io(): CompletableTransformer = CompletableTransformer {
        it.subscribeOn(network).observeOn(main)
    }

    fun computation(): CompletableTransformer = CompletableTransformer {
        it.subscribeOn(computation).observeOn(main)
    }

    fun main(): CompletableTransformer = CompletableTransformer {
        it.subscribeOn(main).observeOn(main)
    }
}

object ScheduleFlowable : ScheduleTransformers() {
    fun <T> io(): FlowableTransformer<T, T> = FlowableTransformer {
        it.subscribeOn(network).observeOn(main)
    }

    fun <T> computation(): FlowableTransformer<T, T> = FlowableTransformer {
        it.subscribeOn(computation).observeOn(main)
    }

    fun <T> main(): FlowableTransformer<T, T> = FlowableTransformer {
        it.subscribeOn(main).observeOn(main)
    }
}

object ScheduleMaybe : ScheduleTransformers() {

    fun <T> io(): MaybeTransformer<T, T> = MaybeTransformer {
        it.subscribeOn(network).observeOn(main)
    }

    fun <T> computation(): MaybeTransformer<T, T> = MaybeTransformer {
        it.subscribeOn(computation).observeOn(main)
    }

    fun <T> main(): MaybeTransformer<T, T> = MaybeTransformer {
        it.subscribeOn(main).observeOn(main)
    }
}

object ScheduleSingle : ScheduleTransformers() {

    fun <T> io(): SingleTransformer<T, T> = SingleTransformer {
        it.subscribeOn(network).observeOn(main)
    }

    fun <T> computation(): SingleTransformer<T, T> = SingleTransformer {
        it.subscribeOn(computation).observeOn(main)
    }

    fun <T> main(): SingleTransformer<T, T> = SingleTransformer {
        it.subscribeOn(main).observeOn(main)
    }

}

object ScheduleObservable : ScheduleTransformers() {

    fun <T> io(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(network).observeOn(main)
    }

    fun <T> computation(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(computation).observeOn(main)
    }

    fun <T> main(): ObservableTransformer<T, T> = ObservableTransformer {
        it.subscribeOn(main).observeOn(main)
    }
}

fun <T> Single<T>.scheduleComputation(): Single<T> = compose(ScheduleSingle.computation())
fun <T> Single<T>.scheduleIO(): Single<T> = compose(ScheduleSingle.io())
fun <T> Single<T>.scheduleMain(): Single<T> = compose(ScheduleSingle.main())
fun <T> Maybe<T>.scheduleIO(): Maybe<T> = compose(ScheduleMaybe.io())
fun <T> Maybe<T>.scheduleComputation(): Maybe<T> = compose(ScheduleMaybe.computation())
fun <T> Maybe<T>.scheduleMain(): Maybe<T> = compose(ScheduleMaybe.main())
fun <T> Observable<T>.scheduleIO(): Observable<T> = compose(ScheduleObservable.io())
fun <T> Observable<T>.scheduleComputation(): Observable<T> = compose(ScheduleObservable.computation())
fun <T> Observable<T>.scheduleMain(): Observable<T> = compose(ScheduleObservable.main())
fun <T> Flowable<T>.scheduleComputation(): Flowable<T> = compose(ScheduleFlowable.computation())
fun <T> Flowable<T>.scheduleIO(): Flowable<T> = compose(ScheduleFlowable.io())
fun <T> Flowable<T>.scheduleMain(): Flowable<T> = compose(ScheduleFlowable.main())
fun Completable.scheduleComputation(): Completable = compose(ScheduleCompletable.computation())
fun Completable.scheduleIO(): Completable = compose(ScheduleCompletable.io())
fun Completable.scheduleMain(): Completable = compose(ScheduleCompletable.main())

fun <T> Single<T>.subscribe(e: SingleEmitter<T>): Disposable = subscribe(e::onSuccess, e::onError)
fun <T> Observable<T>.subscribe(e: ObservableEmitter<T>): Disposable = subscribe(e::onNext, e::onError, e::onComplete)
fun <T> Flowable<T>.subscribe(e: FlowableEmitter<T>): Disposable = subscribe(e::onNext, e::onError, e::onComplete)
fun <T> Maybe<T>.subscribe(e: MaybeEmitter<T>): Disposable = subscribe(e::onSuccess, e::onError, e::onComplete)
fun Completable.subscribe(e: CompletableEmitter): Disposable = subscribe(e::onComplete, e::onError)

fun <T, C : Collection<T>> Single<C>.iterate(): Observable<T> = flatMapObservable { Observable.fromIterable(it) }
fun <T, C : Collection<T>> Maybe<C>.iterate(): Observable<T> = flatMapObservable { Observable.fromIterable(it) }
