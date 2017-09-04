package com.chadbingham.loyautils.fire

import com.chadbingham.loyautils.rx.Event
import com.google.firebase.database.*
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Executors

object ReferenceProvider {
    var reference: DatabaseReference? = null
}

interface Mapper<T, R> {
    fun map(t: T): R
}

interface SnapshotMapper<R> : Mapper<DataSnapshot, R> {
    override fun map(snapshot: DataSnapshot): R
}

class Mappers {
    companion object {
        val NONE = object : SnapshotMapper<DataSnapshot> {
            override fun map(snapshot: DataSnapshot): DataSnapshot = snapshot
        }

        val STRING_SET = object : SnapshotMapper<Set<String>> {
            override fun map(snapshot: DataSnapshot): Set<String> {
                val keys = mutableSetOf<String>()
                if (snapshot.exists()) {
                    if (snapshot.hasChildren()) {
                        snapshot.children
                                .filter { it.key != null }
                                .mapTo(keys) { it.key }
                    } else {
                        keys.add(snapshot.key)
                    }
                }
                return keys
            }
        }
    }
}

class RxFirebase<T>(private val mapper: SnapshotMapper<T>, private val query: Query) {

    constructor(mapper: SnapshotMapper<T>, vararg children: String) : this(mapper,
            ReferenceProvider.reference!!.child(children.joinToString(separator = "/")))

    fun keepSynced() = query.keepSynced(true)

    fun <R> mapper(mapper: SnapshotMapper<R>): RxFirebase<R> {
        return RxFirebase(mapper, query)
    }

    fun noMapper(): RxFirebase<DataSnapshot> {
        return RxFirebase(Mappers.NONE, query)
    }

    fun child(child: String): RxFirebase<T> {
        return RxFirebase(mapper, query.ref.child(child))
    }

    fun startAt(child: String, value: Any?): RxFirebase<T> {
        return value?.let {
            when (it) {
                is Double -> RxFirebase(mapper, query.startAt(it, child))

                is Boolean -> RxFirebase(mapper, query.startAt(it, child))

                else -> RxFirebase(mapper, query.startAt(it as String, child))
            }
        } ?: RxFirebase(mapper, query)
    }

    fun searchBy(orderByChild: String, text: String): RxFirebase<T> {
        val first = text[0].toString()
        return RxFirebase(mapper, query
                .orderByChild(orderByChild)
                .startAt(first)
                .endAt(first + "~"))
    }

    fun startAt(key: String?): RxFirebase<T> {
        return if (key != null && key.isNotBlank()) {
            RxFirebase(mapper, query.startAt(key))
        } else RxFirebase(mapper, query)
    }

    fun startAt(key: Char): RxFirebase<T> {
        return RxFirebase(mapper, query.startAt(key.toString()))
    }

    fun endAt(key: String?): RxFirebase<T> {
        return if (key != null && key.isNotBlank()) {
            RxFirebase(mapper, query.endAt(key))
        } else RxFirebase(mapper, query)
    }

    fun startAt(child: Double, key: String): RxFirebase<T> {
        return RxFirebase(mapper, query.startAt(child, key))
    }

    fun orderByChild(key: String): RxFirebase<T> {
        return RxFirebase(mapper, query.orderByChild(key))
    }

    fun limitToLast(limit: Int): RxFirebase<T> {
        return RxFirebase(mapper, query.limitToLast(limit))
    }

    fun limitToFirst(limit: Int): RxFirebase<T> {
        return RxFirebase(mapper, query.limitToFirst(limit))
    }

    fun generateKey(): String {
        return query.ref.push().key
    }

    fun removeValue(): Completable {
        return Completable
                .create { e ->
                    query.ref.removeValue()
                            .addOnCompleteListener({ e.onComplete() })
                            .addOnFailureListener({ e.onError(it) })
                }
                .compose(Schedule.completable())
    }

    fun setValue(any: Any): Completable {
        return Completable
                .create { e ->
                    query.ref.setValue(any)
                            .addOnCompleteListener({ e.onComplete() })
                            .addOnFailureListener({ e.onError(it) })
                }
                .compose(Schedule.completable())
    }

    fun countChildren(): Single<Long> = FireListeners
            .singleValueListener(query)
            .filter { it.exists() }
            .filter { it.hasChildren() }
            .map { it.childrenCount }
            .doOnError { e -> Timber.e("countChildren, path: %s\n%s", query, e.message) }
            .toSingle(0L)

    fun childrenListener(): Observable<T> = FireListeners
            .valueListener(query)
            .map { it.children }
            .flatMap { Observable.fromIterable(it) }
            .map { mapper.map(it) }

    fun children(): Observable<T> = FireListeners
            .singleValueListener(query)
            .filter { it.exists() }
            .map { it.children }
            .flatMapObservable { Observable.fromIterable(it) }
            .map { mapper.map(it) }

    val valueListener: Observable<T>
        get() = FireListeners.valueListener(query).map { mapper.map(it) }

    val singleListener: Single<T>
        get() = FireListeners
                .singleValueListener(query)
                .toObservable()
                .filter { it.exists() }
                .filter { it.hasChildren() }
                .map { mapper.map(it) }
                .singleOrError()
                .doOnError({ e -> Timber.e("getSingleListener: Path:%1\$s %2\$s", query, e.message) })

    val maybeListener: Maybe<T>
        get() = FireListeners.singleValueListener(query)
                .filter({ ds ->
                    if (!ds.exists()) {
                        Timber.v("DataSnapshot doesn't exist: %s", ds.key)
                        false
                    } else if (!ds.hasChildren()) {
                        Timber.v("DataSnapshot doesn't have children: %s", ds.key)
                        false
                    } else {
                        true
                    }
                })
                .map { mapper.map(it) }
                .doOnError({ e -> Timber.e("getMaybeListener: Path:%1\$s %2\$s", query, e.message) })

    val childEventListener: Flowable<Event<T>>
        get() = FireListeners.childEventListener(query)
                .map { it.map(mapper) }
                .doOnError({ e -> Timber.e("getChildEventListener: Path:%1\$s %2\$s", query, e.message) })

    fun log(): RxFirebase<T> {
        Timber.d("query=$query")
        return this
    }

    fun log(message: String): RxFirebase<T> {
        Timber.d("$message: query=$query")
        return this
    }
}

internal object FireListeners {

    fun valueListener(query: Query): Observable<DataSnapshot> {
        return Observable
                .create<DataSnapshot>({ e ->
                    query.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            e.onNext(ds)
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            if (ex.toException() != null) {
                                Timber.e("onCancelled: " + ex)
                            }
                            e.onComplete()
                        }
                    })
                })
                .compose(Schedule.observable())
    }

    fun singleValueListener(query: Query): Single<DataSnapshot> {
        return Single
                .create<DataSnapshot>({ e ->
                    query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            e.onSuccess(ds)
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            e.onError(ex.toException())
                        }
                    })
                })
                .compose(Schedule.single())
    }

    fun childEventListener(query: Query): Flowable<Event<DataSnapshot>> {
        return Flowable
                .create<Event<DataSnapshot>>({ e ->
                    val listener = object : ChildEventListener {
                        override fun onChildMoved(ds: DataSnapshot?, p1: String?) {
                            ds?.let {
                                if (it.exists()) {
                                    e.onNext(Event.Changed(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildAdded(ds: DataSnapshot?, s: String) {
                            ds?.let {
                                if (it.exists()) {
                                    e.onNext(Event.Added(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildChanged(ds: DataSnapshot?, s: String) {
                            ds?.let {
                                if (ds.exists()) {
                                    e.onNext(Event.Changed(ds))
                                } else {
                                    e.onNext(Event.Empty())
                                }
                            }
                        }

                        override fun onChildRemoved(ds: DataSnapshot?) {
                            ds?.let {
                                e.onNext(Event.Removed(it))
                            }
                        }

                        override fun onCancelled(ex: DatabaseError) {
                            e.onNext(Event.Canceled(ex.toException()))
                        }
                    }

                    query.addChildEventListener(listener)
                    e.setCancellable { query.removeEventListener(listener) }
                }, BackpressureStrategy.BUFFER)
                .compose(Schedule.flowable())
    }
}

internal object Schedule {
    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    fun <T> flowable(): FlowableTransformer<T, T> {
        return FlowableTransformer { upstream -> upstream.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
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
