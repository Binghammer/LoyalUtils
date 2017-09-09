@file:Suppress("MemberVisibilityCanPrivate", "unused")

package com.chadbingham.loyautils.fire

import com.chadbingham.loyautils.rx.Event
import com.google.firebase.database.*
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.CompletableSubject
import timber.log.Timber
import java.util.concurrent.Executors

object FirebaseReferenceProvider {
    var database: FirebaseDatabase? = FirebaseDatabase.getInstance()
    var reference = database?.reference
        private set
}

interface Mapper<in T, out R> {
    fun map(t: T): R?
}

interface SnapshotMapper<out R> : Mapper<DataSnapshot, R> {
    override fun map(t: DataSnapshot): R?
}

class Mappers {
    companion object {

        fun <T> DEFAULT(clazz: Class<T>): SnapshotMapper<T> {
            return object : SnapshotMapper<T> {
                override fun map(t: DataSnapshot): T? {
                    return t.getValue(clazz)
                }
            }
        }

        val NONE = object : SnapshotMapper<DataSnapshot> {
            override fun map(t: DataSnapshot): DataSnapshot = t
        }

        val LONG = object : SnapshotMapper<Long> {
            override fun map(t: DataSnapshot): Long {
                return t.getValue(true) as Long
            }
        }

        val INT = object : SnapshotMapper<Int> {
            override fun map(t: DataSnapshot): Int {
                return t.getValue(true) as Int
            }
        }

        val STRING = object : SnapshotMapper<String> {
            override fun map(t: DataSnapshot): String {
                return t.getValue(true) as String
            }
        }

        val BOOLEAN = object: SnapshotMapper<Boolean> {
            override fun map(t: DataSnapshot): Boolean {
                return t.getValue(true) as Boolean
            }
        }

        val STRING_SET = object : SnapshotMapper<Set<String>> {
            override fun map(t: DataSnapshot): Set<String> {
                val keys = mutableSetOf<String>()
                if (t.exists()) {
                    if (t.hasChildren()) {
                        t.children
                                .filter { it.key != null }
                                .mapTo(keys) { it.key }
                    } else {
                        keys.add(t.key)
                    }
                }
                return keys
            }
        }
    }
}

class RxFirebase<T>(private val mapper: SnapshotMapper<T>, private val query: Query) {

    constructor(mapper: SnapshotMapper<T>, vararg children: String) : this(mapper,
            FirebaseReferenceProvider.reference!!.child(children.joinToString(separator = "/")))

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

    fun startAt(key: String): RxFirebase<T> {
        return if (key.isNotBlank()) {
            RxFirebase(mapper, query.startAt(key))
        } else RxFirebase(mapper, query)
    }

    fun endAt(key: String): RxFirebase<T> {
        return if (key.isNotBlank()) {
            RxFirebase(mapper, query.endAt(key))
        } else RxFirebase(mapper, query)
    }

    fun startAt(child: Double, key: String): RxFirebase<T> {
        return RxFirebase(mapper, query.startAt(child, key))
    }

    fun push(): RxFirebase<T> {
        return RxFirebase(mapper, query.ref.push())
    }

    fun push(t: T): String {
        val ref = query.ref.push()
        val key = ref.key
        ref.setValue(t)
        return key
    }

    fun pushValue(t: T): String {
        val ref = query.ref.push()
        val key = ref.key
        ref.setValue(t)
        return key
    }

    fun orderByKey(): RxFirebase<T> {
        return RxFirebase(mapper, query.orderByKey())
    }

    fun orderByValue(): RxFirebase<T> {
        return RxFirebase(mapper, query.orderByValue())
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
        //return subject so subscribing isn't required to execute
        val subject = CompletableSubject.create()
        query.ref.removeValue()
                .addOnCompleteListener({ subject.onComplete() })
                .addOnFailureListener({ subject.onError(it) })
        return subject
    }

    fun setValue(any: Any): Completable {
        //return subject so subscribing isn't required to execute
        val subject = CompletableSubject.create()
        query.ref.setValue(any)
                .addOnCompleteListener({
                    if (!subject.hasComplete() && !subject.hasThrowable()) {
                        if (it.isSuccessful) {
                            subject.onComplete()
                        } else {
                            if (it.exception != null) {
                                subject.onError(it.exception!!)
                            } else {
                                subject.onError(Exception())
                            }
                        }
                    }
                })
                .addOnSuccessListener({
                    if (!subject.hasComplete() && !subject.hasThrowable())
                        subject.onComplete()
                })
                .addOnFailureListener({ subject.onError(it) })
        return subject
    }

    fun countChildren(): Single<Long> = FireListeners
            .singleValueListener(query)
            .filter { it.exists() }
            .filter { it.hasChildren() }
            .map { it.childrenCount }
            .doOnError { e -> Timber.e("countChildren, path: %s\n%s", query, e.message) }
            .toSingle(0L)

    fun childrenListener(): Flowable<T> = FireListeners
            .valueListener(query)
            .map { it.children }
            .flatMap { Flowable.fromIterable(it) }
            .map { mapper.map(it) }

    fun children(): Flowable<T> = FireListeners
            .singleValueListener(query)
            .filter { it.exists() }
            .map { it.children }
            .flatMapPublisher { Flowable.fromIterable(it) }
            .map { mapper.map(it) }

    val valueListener: Flowable<T>
        get() = FireListeners.valueListener(query).map { mapper.map(it) }

    val singleListener: Single<T>
        get() = FireListeners
                .singleValueListener(query)
                .toObservable()
                .filter { it.exists() }
                .filter { it.hasChildren() }
                .map { mapper.map(it) ?: throw Exception("Object not found at ${query.ref}") }
                .singleOrError()
                .doOnError({ e -> Timber.e("getSingleListener: Path:%1\$s %2\$s", query, e.message) })

    val maybeListener: Maybe<T>
        get() = FireListeners.singleValueListener(query)
                .filter({ ds ->
                    if (!ds.exists()) {
                        Timber.v("DataSnapshot doesn't exist in path ${query.ref}")
                        false
                    } else if (!ds.hasChildren()) {
                        Timber.v("DataSnapshot doesn't have children in path ${query.ref}")
                        false
                    } else if (ds.getValue() == null) {
                        Timber.v("DataSnapshot doesn't have value in path ${query.ref}")
                        false
                    } else {
                        true
                    }
                })
                .map { mapper.map(it)!! }
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

    fun valueListener(query: Query): Flowable<DataSnapshot> {
        return Flowable
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
                }, BackpressureStrategy.BUFFER)
                .compose(Schedule.flowable())
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

    fun <T> single(): SingleTransformer<T, T> {
        return SingleTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }

    fun completable(): CompletableTransformer {
        return CompletableTransformer { o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()) }
    }
}
