package com.chadbingham.loyautils.fire

import android.util.Log
import com.chadbingham.loyautils.rx.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber

class Fire<T>(private val clazz: Class<T>, private val query: Query) {

    constructor(clazz: Class<T>) : this(clazz, FirebaseReferenceProvider.reference)

    constructor(clazz: Class<T>, vararg children: String)
            : this(clazz, FirebaseReferenceProvider.reference.child(children.joinToString(separator = "/")))

    var reader: Reader<T> = { it.getValue(clazz) }
    var writer: Writer<T> = { reference, value -> reference.setValue(value) }
    var pusher: Pusher<T> = { reference, value ->
        reference.setValue(value)
        value
    }

    private val tag: String = "firebase/${clazz.simpleName}"
    private var debugLog = false

    fun enableLogging() {
        debugLog = true
        logDebug("Logging enabled")
    }

    fun query(buildQuery: (query: Query) -> Query): Fire<T> {
        return Fire(clazz, buildQuery(query))
    }

    fun pushValue(value: T): T {
        val ref = query.ref.push()
        logDebug("pushValue at $ref with: $value")
        return pusher(ref, value)
    }

    fun setValue(value: T) {
        logDebug("setValue at ${query.ref} with: $value")
        writer(query.ref, value)
    }

    fun removeValue() {
        logDebug("removeValue at ${query.ref}")
        query.ref.removeValue()
    }

    fun getValue(): Single<T> {
        return RxFirebaseAdapter
                .singleValueListener(query)
                .flatMap { if (it.exists() && it.hasChildren()) Single.just(it) else Single.error(NoValueFound(query)) }
                .flatMap { reader(it)?.let { Single.just(it) } ?: Single.error<T>(NoValueFound(query)) }
    }

    fun getValueSafe(): Maybe<T> {
        return RxFirebaseAdapter.singleValueListener(query)
                .filter({ ds ->
                    if (!ds.exists()) {
                        Timber.v("DataSnapshot doesn't exist in path ${query.ref}")
                        false
                    } else if (!ds.hasChildren()) {
                        Timber.v("DataSnapshot doesn't have children in path ${query.ref}")
                        false
                    } else if (ds.value == null) {
                        Timber.v("DataSnapshot doesn't have value in path ${query.ref}")
                        false
                    } else {
                        true
                    }
                })
                .flatMap {
                    reader(it)?.let { Maybe.just(it) } ?: Maybe.empty()
                }
    }

    fun getValues(): Flowable<T> {
        logDebug("getChildren at ${query.ref}")
        return RxFirebaseAdapter
                .singleValueListener(query)
                .filter { it.exists() }
                .map { it.children }
                .flatMapPublisher { Flowable.fromIterable(it) }
                .map { reader(it) }
    }

    fun getValueEventListener(): Flowable<Event<T>> {
        logDebug("getChildEventListener: ${query.ref}")
        return RxFirebaseAdapter.childEventListener(query)
                .map {
                    val value = it.value?.let { reader(it) }
                    when (it) {
                        is Event.Added -> AddedEvent(value!!)

                        is Event.Changed -> ChangedEvent(value!!)

                        is Event.Removed -> RemovedEvent(value)

                        is Event.Empty -> EmptyEvent()

                        is Event.Canceled -> CancelledEvent()

                        else -> error("Unknown Event: $it")
                    }
                }
                .doOnError({ e ->
                    logError("getChildEventListener: \n\tPath=$query \n\tERROR=${e.message}")
                })

    }

    private fun logDebug(message: String) {
        if (debugLog)
            Log.d(tag, message)
    }

    private fun logWarning(message: String) {
        Log.w(tag, message)
    }

    private fun logError(message: String) {
        Log.e(tag, message)
    }
}

class NoValueFound(query: Query) : Throwable("No value found in path: ${query.ref}")

typealias Writer<T> = (reference: DatabaseReference, value: T) -> Unit
typealias Pusher<T> = (reference: DatabaseReference, value: T) -> T
typealias Reader<T> = (snapshot: DataSnapshot) -> T?