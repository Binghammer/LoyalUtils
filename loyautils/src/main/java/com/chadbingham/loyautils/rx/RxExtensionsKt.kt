package com.chadbingham.loyautils.rx

import io.reactivex.*

fun <T, C : Collection<T>> Single<C>.iterate(): Observable<T> {
    return flatMapObservable { list -> Observable.fromIterable(list) }
}

fun <T> Single<T>.computationSingle(): Single<T> {
    return this.compose(ScheduleSingle.computation())
}

fun <T> Single<T>.scheduleSingleIO(): Single<T> {
    return this.compose(ScheduleSingle.io())
}

fun <T> Single<T>.scheduleSingleComputation(): Single<T> {
    return this.compose(ScheduleSingle.computation())
}

fun <T> Maybe<T>.scheduleMaybeIO(): Maybe<T> {
    return this.compose(ScheduleMaybe.io())
}

fun <T> Maybe<T>.scheduleMaybeComputation(): Maybe<T> {
    return this.compose(ScheduleMaybe.computation())
}

fun <T> Observable<T>.scheduleObservableIO(): Observable<T> {
    return this.compose(ScheduleObservable.io())
}

fun <T> Observable<T>.scheduleObservableComputation(): Observable<T> {
    return this.compose(ScheduleObservable.computation())
}

fun <T> Flowable<T>.scheduleFlowableComputation(): Flowable<T> {
    return this.compose(ScheduleFlowable.computation())
}

fun Completable.scheduleCompletableComputation(): Completable {
    return this.compose(ScheduleCompletable.computation())
}

fun Completable.scheduleCompletableIo(): Completable {
    return this.compose(ScheduleCompletable.io())
}

fun Completable.scheduleCompletableMain(): Completable {
    return this.compose(ScheduleCompletable.main())
}

fun <T> Single<T>.subscribeWithEmitter(e: SingleEmitter<T>) {
    subscribe({ e.onSuccess(it) }, { e.onError(it) })
}

fun <T> Maybe<T>.subscribeWithEmitter(e: MaybeEmitter<T>) {
    subscribe({ e.onSuccess(it) }, { e.onError(it) }, { e.onComplete() })
}

fun Completable.subscribeWithEmitter(e: CompletableEmitter) {
    subscribe({ e.onComplete() }, { e.onError(it) })
}
