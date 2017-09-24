@file:Suppress("unused")

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

object Fire {
    internal var debugLogging = false

    fun enableLogging(log: Boolean) {
        debugLogging = log
    }

    inline fun <reified T> reader(vararg children: String): FireReader<T> {
        return reader(Fire.joinToReference(*children))
    }

    inline fun <reified T> reader(reference: DatabaseReference = FirebaseReferenceProvider.reference): FireReader<T> {
        return reader(T::class.java, reference)
    }

    inline fun <reified T> reader(query: Query = FirebaseReferenceProvider.reference): FireReader<T> {
        return reader(T::class.java, query)
    }

    fun <T> reader(clazz: Class<T>, query: Query): FireReader<T> {
        return FireReaderImpl(clazz, query)
    }

    inline fun <reified T> writer(reference: DatabaseReference = FirebaseReferenceProvider.reference): FireWriter<T> {
        return writer(T::class.java, reference)
    }

    inline fun <reified T> writer(vararg children: String): FireWriter<T> {
        return writer(Fire.joinToReference(*children))
    }

    fun <T> writer(clazz: Class<T>, reference: DatabaseReference): FireWriter<T> {
        return FireWriterImpl(clazz, reference)
    }

    inline fun <reified T> readerWriter(vararg children: String): FireReaderWriter<T> {
        return readerWriter(Fire.joinToReference(*children))
    }

    inline fun <reified T> readerWriter(reference: DatabaseReference = FirebaseReferenceProvider.reference): FireReaderWriter<T> {
        return readerWriter(T::class.java, reference)
    }

    fun <T> readerWriter(clazz: Class<T>, reference: DatabaseReference = FirebaseReferenceProvider.reference): FireReaderWriter<T> {
        return FireReaderWriter(clazz, reference)
    }

    fun joinToReference(vararg children: String): DatabaseReference {
        return FirebaseReferenceProvider.reference.child(children.joinToString(separator = "/"))
    }
}

interface FireWriter<T> {
    var writer: Writer<T>
    var pusher: Pusher<T>?
    fun child(vararg children: String): FireWriter<T>
    fun pushValue(value: T): T
    fun setValue(value: T)
    fun removeValue()
}

interface FireReader<T> {
    var reader: Reader<T>
    fun query(buildQuery: (query: Query) -> Query): FireReader<T>
    fun getValue(): Single<T>
    fun getValueSafe(): Maybe<T>
    fun getValues(): Flowable<T>
    fun getValueEventListener(): Flowable<Event<T>>
}

class FireReaderWriter<T> internal constructor(
        private val clazz: Class<T>,
        private val reference: DatabaseReference = FirebaseReferenceProvider.reference,
        private val fireWriter: FireWriter<T> = FireWriterImpl(clazz, reference),
        private val fireReader: FireReader<T> = FireReaderImpl(clazz, reference))
    : FireWriter<T> by fireWriter, FireReader<T> by fireReader

internal class FireWriterImpl<T>(clazz: Class<T>, reference: DatabaseReference) : FireAbstract<T>(clazz, reference), FireWriter<T> {

    override var writer: Writer<T> = { reference, value -> reference.setValue(value) }
    override var pusher: Pusher<T>? = { reference, value ->
        reference.setValue(value)
        value
    }

    override fun child(vararg children: String): FireWriterImpl<T> {
        return FireWriterImpl(clazz, Fire.joinToReference(*children))
    }

    override fun pushValue(value: T): T {
        val ref = reference.push()
        logDebug("pushValue at $ref with: $value")
        return if (pusher != null) {
            pusher!!.invoke(ref, value)
        } else {
            writer(ref, value)
            value
        }
    }

    override fun setValue(value: T) {
        logDebug("setValue at $reference with: $value")
        writer(reference, value)
    }

    override fun removeValue() {
        logDebug("removeValue at $reference")
        reference.removeValue()
    }
}

internal class FireReaderImpl<T>(clazz: Class<T>, private val query: Query) : FireAbstract<T>(clazz, query.ref), FireReader<T> {

    override var reader: Reader<T> = { it.getValue(clazz) }

    override fun query(buildQuery: (query: Query) -> Query): FireReaderImpl<T> {
        return FireReaderImpl(clazz, buildQuery(query))
    }

    override fun getValue(): Single<T> {
        return RxFirebaseAdapter
                .singleValueListener(query)
                .flatMap { if (it.exists() && it.hasChildren()) Single.just(it) else Single.error(NoValueFound(query)) }
                .flatMap { reader(it)?.let { Single.just(it) } ?: Single.error<T>(NoValueFound(query)) }
    }

    override fun getValueSafe(): Maybe<T> {
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

    override fun getValues(): Flowable<T> {
        logDebug("getChildren at ${query.ref}")
        return RxFirebaseAdapter
                .singleValueListener(query)
                .filter { it.exists() }
                .map { it.children }
                .flatMapPublisher { Flowable.fromIterable(it) }
                .map { reader(it) }
    }

    override fun getValueEventListener(): Flowable<Event<T>> {
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
}

internal open class FireAbstract<T>(protected val clazz: Class<T>, protected val reference: DatabaseReference) {

    private val tag: String = "firebase/${clazz.simpleName}"

    protected fun logDebug(message: String) {
        if (Fire.debugLogging)
            Log.d(tag, message)
    }

    protected fun logWarning(message: String) {
        Log.w(tag, message)
    }

    protected fun logError(message: String) {
        Log.e(tag, message)
    }
}

class NoValueFound(query: Query) : Throwable("No value found in path: ${query.ref}")

typealias Writer<T> = (reference: DatabaseReference, value: T) -> Unit
typealias Pusher<T> = (reference: DatabaseReference, value: T) -> T
typealias Reader<T> = (snapshot: DataSnapshot) -> T?