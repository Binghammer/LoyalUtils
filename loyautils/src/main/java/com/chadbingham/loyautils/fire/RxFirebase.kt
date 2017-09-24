@file:Suppress("MemberVisibilityCanPrivate", "unused")

package com.chadbingham.loyautils.fire

import com.chadbingham.loyautils.rx.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import timber.log.Timber

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

        val KEYS = object : SnapshotMapper<String> {
            override fun map(t: DataSnapshot): String {
                return t.key
            }
        }

        val STRING = object : SnapshotMapper<String> {
            override fun map(t: DataSnapshot): String {
                return t.getValue(true) as String
            }
        }

        val BOOLEAN = object : SnapshotMapper<Boolean> {
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
            FirebaseReferenceProvider.reference.child(children.joinToString(separator = "/")))

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

    fun countChildren(): Single<Long> = RxFirebaseAdapter
            .singleValueListener(query)
            .filter { it.exists() }
            .filter { it.hasChildren() }
            .map { it.childrenCount }
            .doOnError { e -> Timber.e("countChildren, path: %s\n%s", query, e.message) }
            .toSingle(0L)

    fun childrenListener(): Flowable<T> = RxFirebaseAdapter
            .valueListener(query)
            .map { it.children }
            .flatMap { Flowable.fromIterable(it) }
            .map { mapper.map(it) }

    fun children(): Flowable<T> = RxFirebaseAdapter
            .singleValueListener(query)
            .filter { it.exists() }
            .map { it.children }
            .flatMapPublisher { Flowable.fromIterable(it) }
            .map { mapper.map(it) }

    val valueListener: Flowable<T>
        get() = RxFirebaseAdapter.valueListener(query).map { mapper.map(it) }

    val singleListener: Single<T>
        get() = RxFirebaseAdapter
                .singleValueListener(query)
                .toObservable()
                .filter { it.exists() }
                .filter { it.hasChildren() }
                .map { mapper.map(it) ?: throw Exception("Object not found at ${query.ref}") }
                .singleOrError()
                .doOnError({ e -> Timber.e("getSingleListener: Path:%1\$s %2\$s", query, e.message) })

    val maybeListener: Maybe<T>
        get() = RxFirebaseAdapter.singleValueListener(query)
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
        get() = RxFirebaseAdapter.childEventListener(query)
                .map {
                    val value = it.value?.let { mapper.map(it) }
                    when (it) {
                        is Event.Added -> AddedEvent(value!!)

                        is Event.Changed -> ChangedEvent(value!!)

                        is Event.Removed -> RemovedEvent(value)

                        is Event.Empty -> EmptyEvent()

                        is Event.Canceled -> CancelledEvent()

                        else -> error("Unknown Event: $it")
                    }
                }
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