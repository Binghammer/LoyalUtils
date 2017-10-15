package com.chadbingham.loyautils.misc

import android.support.v7.util.SortedList
import io.reactivex.Completable

fun <T> MutableList<T>.clearAndSet(list: Collection<T>) {
    clear()
    addAll(list)
}

val <T> List<T>.lastItem: T?
    get() = if (isNotEmpty()) this[lastIndex] else null

/**
 * @return true if object was added and did not exist. False if it replaced an item
 */
fun <T> MutableList<T>.addUnique(t: T, unique: (t: T) -> Boolean): Boolean {
    for (i in 0 until size) {
        if (unique.invoke(this[i])) {
            this[i] = t
            return false
        }
    }
    add(t)
    return false
}

fun <T> MutableList<T>.removeWhere(where: (t: T) -> Boolean): Int {
    val found = (0 until size)
            .map { this[it] }
            .filter { where.invoke(it) }

    removeAll(found)
    return found.size
}

fun <T> List<T>.copy() = toList()

fun <T> List<T>.mutableCopy() = toMutableList()

fun <T> ArrayList<T>.copy(): ArrayList<T> {
    val copy = arrayListOf<T>()
    copy.addAll(this)
    return copy
}

fun <T> ArrayList<T>.mutableCopy(): MutableList<T> {
    val copy = mutableListOf<T>()
    copy.addAll(this)
    return copy
}

fun <T> MutableList<T>.replace(t: T, where: (t: T) -> Boolean) {
    forEachIndexed { index, item ->
        if (where.invoke(item)) {
            set(index, t)
        }
    }
}

fun <T> SortedList<T>.indexAt(where: (t: T) -> Boolean): Int {
    return (0 until size()).firstOrNull { where(this[it]) }
            ?: -1
}

fun <T> SortedList<T>.removeFirst(where: (t: T) -> Boolean): T? {
    val r = (0 until size())
            .map { get(it) }
            .firstOrNull { where.invoke(it) }
    r?.let { remove(it) }
    return r
}

fun <T> SortedList<T>.removeWhere(where: (t: T) -> Boolean) {
    beginBatchedUpdates()

    val remove = (0 until size())
            .map { get(it) }
            .filter { where.invoke(it) }

    remove.forEach { remove(it) }

    endBatchedUpdates()
}

/**
 * Removes all elements from this MutableIterable that match the given predicate
 */
fun <T> MutableIterable<T>.retainAll(predicate: (T) -> Boolean): MutableList<T> = filterInPlace(predicate, false)

/**
 * Removes all elements from this MutableIterable
 */
fun <T> MutableIterable<T>.extractAll(): MutableList<T> {
    return extractAll { true }
}

/**
 * Removes all elements from this MutableIterable that match the given predicate
 */
fun <T> MutableIterable<T>.extractAll(predicate: (T) -> Boolean): MutableList<T> = filterInPlace(predicate, true)

private fun <T> MutableIterable<T>.filterInPlace(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): MutableList<T> {
    val result = mutableListOf<T>()
    with(iterator()) {
        while (hasNext()) {
            val t = next()
            if (predicate(t) == predicateResultToRemove) {
                remove()
                result.add(t)
            }
        }
    }
    return result
}

fun <T> Iterable<T>.indexOf(predicate: (T) -> Boolean): Int {
    this.forEachIndexed { index, t ->
        if (predicate(t)) {
            return index
        }
    }
    return -1
}

fun <T> SortedList<T>.firstOrNull(filter: (t: T) -> Boolean): T? {
    return (0 until size())
            .map { get(it) }
            .firstOrNull { filter.invoke(it) }
}

fun <T> Iterable<T>.mergeDelayError(transform: (t: T) -> Completable): Completable {
    return map { transform(it) }.mergeDelayError()
}

fun Iterable<Completable>.mergeDelayError(): Completable {
    return Completable.mergeDelayError(this)
}
