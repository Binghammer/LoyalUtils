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

    inline fun <reified T> reader(vararg children: String): FirebaseReader<T> {
        return reader(Fire.joinToReference(*children))
    }

    inline fun <reified T> reader(reference: DatabaseReference = FirebaseReferenceProvider.reference): FirebaseReader<T> {
        return reader(T::class.java, reference)
    }

    inline fun <reified T> reader(query: Query = FirebaseReferenceProvider.reference): FirebaseReader<T> {
        return reader(T::class.java, query)
    }

    fun <T> reader(clazz: Class<T>, query: Query): FirebaseReader<T> {
        return FirebaseReaderImpl(clazz, query)
    }

    inline fun <reified T> writer(reference: DatabaseReference = FirebaseReferenceProvider.reference): FirebaseWriter<T> {
        return writer(T::class.java, reference)
    }

    inline fun <reified T> writer(vararg children: String): FirebaseWriter<T> {
        return writer(Fire.joinToReference(*children))
    }

    fun <T> writer(clazz: Class<T>, reference: DatabaseReference): FirebaseWriter<T> {
        return FirebaseWriterImpl(clazz, reference)
    }

    inline fun <reified T> readerWriter(vararg children: String): FireReaderWriter<T> {
        return readerWriter(Fire.joinToReference(*children))
    }

    inline fun <reified T> readerWriter(reference: DatabaseReference = FirebaseReferenceProvider.reference): FireReaderWriter<T> {
        return FireReaderWriter(T::class.java, reference)
    }

    fun joinToReference(vararg children: String): DatabaseReference {
        return FirebaseReferenceProvider.reference.child(children.joinToString(separator = "/"))
    }
}

interface FirebaseWriter<T> {
    var writer: Writer<T>
    var pusher: Pusher<T>?
    fun child(vararg children: String): FirebaseWriter<T>
    fun pushValue(value: T): T
    fun setValue(value: T)
    fun removeValue()
}

interface FirebaseReader<T> {
    var reader: Reader<T>
    fun query(buildQuery: (query: Query) -> Query): FirebaseReader<T>
    fun getValue(): Single<T>
    fun getValueSafe(): Maybe<T>
    fun getValues(): Flowable<T>
    fun getValueEventListener(): Flowable<Event<T>>
}

class FireReaderWriter<T>(
        private val clazz: Class<T>,
        private val reference: DatabaseReference = FirebaseReferenceProvider.reference,
        private val fireWriter: FirebaseWriter<T> = FirebaseWriterImpl(clazz, reference),
        private val fireReader: FirebaseReader<T> = FirebaseReaderImpl(clazz, reference))
    : FirebaseWriter<T> by fireWriter, FirebaseReader<T> by fireReader

internal class FirebaseWriterImpl<T>(clazz: Class<T>, reference: DatabaseReference)
    : BaseFire<T>(clazz, reference), FirebaseWriter<T> {

    override var writer: Writer<T> = { reference, value -> reference.setValue(value) }
    override var pusher: Pusher<T>? = { reference, value ->
        reference.setValue(value)
        value
    }

    override fun child(vararg children: String): FirebaseWriterImpl<T> {
        return FirebaseWriterImpl(clazz, Fire.joinToReference(*children))
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

internal class FirebaseReaderImpl<T>(clazz: Class<T>, private val query: Query)
    : BaseFire<T>(clazz, query.ref), FirebaseReader<T> {

    override var reader: Reader<T> = { it.getValue(clazz) }

    override fun query(buildQuery: (query: Query) -> Query): FirebaseReaderImpl<T> {
        return FirebaseReaderImpl(clazz, buildQuery(query))
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

internal open class BaseFire<T>(protected val clazz: Class<T>, protected val reference: DatabaseReference = FirebaseReferenceProvider.reference) {

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