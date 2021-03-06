package com.chadbingham.loyautils.dialog

import android.app.AlertDialog
import android.content.Context
import android.support.annotation.ArrayRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import com.chadbingham.loyautils.R
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter

data class BooleanDialog(@StringRes val title: Int,
                         @StringRes val message: Int?,
                         @StringRes private val positiveButton: Int,
                         @StringRes private val negativeButton: Int,
                         private val stringMessage: String? = null) {

    fun show(fragment: Fragment): Maybe<Boolean> = fragment.context?.let(::show) ?: Maybe.empty()

    fun show(context: Context): Maybe<Boolean> {
        return show({
            val dialog = AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setPositiveButton(positiveButton) { _, _ -> it.onSuccess(true) }
                    .setNegativeButton(negativeButton) { _, _ -> it.onSuccess(false) }
            if (message != null) {
                dialog.setMessage(message)
            } else if (stringMessage != null) {
                dialog.setMessage(stringMessage)
            }
            dialog
        })
    }
}

object BooleanDialogs {
    fun confirmExit(): BooleanDialog = BooleanDialog(R.string.exit_without_saving_title,
            R.string.exit_without_saving_message,
            R.string.exit,
            R.string.stay)

    fun confirmYesNo(@StringRes title: Int, @StringRes message: Int) =
            BooleanDialog(title,
                    message,
                    R.string.yes,
                    R.string.no)

    fun confirmYesNo(@StringRes title: Int, stringMessage: String) =
            BooleanDialog(title,
                    null,
                    R.string.yes,
                    R.string.no,
                    stringMessage)
}

fun showSingleSelectionDialog(context: Context, @StringRes title: Int, @ArrayRes items: Int): Maybe<Int> {
    return ListDialog(context, title).showSingleSelection(items)
}

fun <T> showSingleSelectionDialog(context: Context, @StringRes title: Int, items: ArrayList<T>, toString: (T) -> CharSequence): Maybe<T> {
    return ListDialog(context, title)
            .showSingleSelection(toCharArray(items, toString))
            .map({ items[it] })
}

fun <T> showSingleSelectionDialog(context: Context, @StringRes title: Int, items: List<T>, toString: (T) -> CharSequence): Maybe<T> {
    return ListDialog(context, title)
            .showSingleSelection(toCharArray(items, toString))
            .map({ items[it] })
}

fun <T> showSingleIndexSelectionDialog(context: Context, @StringRes title: Int, items: List<T>): Maybe<Int> {
    return ListDialog(context, title)
            .showSingleSelection(toCharArray(items, { it.toString() }))
}

fun <T> showSingleSelectionDialog(context: Context, @StringRes title: Int, items: List<T>): Maybe<T> {
    return showSingleIndexSelectionDialog(context, title, items).map({ items[it] })
}

fun <T> showMultiSelectionDialog(context: Context,
                                 @StringRes title: Int,
                                 items: ArrayList<T>,
                                 toString: (T) -> CharSequence,
                                 checkedItems: BooleanArray): Maybe<ArrayList<T>> {
    return ListDialog(context, title)
            .showMultiSelection(toCharArray(items, toString), checkedItems)
            .map({
                val list = arrayListOf<T>()
                val iterator = it.iterator()
                var index = 0
                while (iterator.hasNext()) {
                    if (iterator.nextBoolean()) {
                        list.add(items[index])
                    }
                    index++
                }

                list
            })
}

private class ListDialog(val context: Context, @StringRes val title: Int) {

    val selected = booleanArrayOf()

    fun showSingleSelection(items: Array<CharSequence>): Maybe<Int> {
        return show {
            AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setItems(items) { _, which -> it.onSuccess(which) }
        }
    }

    fun showSingleSelection(@ArrayRes items: Int): Maybe<Int> {
        return show {
            AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setItems(items) { _, which -> it.onSuccess(which) }
        }
    }

    fun showMultiSelection(items: Array<CharSequence>, checkedItems: BooleanArray): Maybe<BooleanArray> {
        return show {
            AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                        selected[which] = isChecked
                    }
                    .setPositiveButton(R.string.OK) { _, _ -> it.onSuccess(selected) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> it.onComplete() }
        }
    }
}

fun <T> show(builder: (emitter: MaybeEmitter<T>) -> AlertDialog.Builder): Maybe<T> {
    return Maybe
            .create<T>({ e ->
                val dialog = builder.invoke(e).create()
                dialog.window.attributes.windowAnimations = R.style.AlertDialogAnimations
                dialog.setOnDismissListener { e.onComplete() }
                dialog.show()
            })
}

private fun <T> toCharArray(items: Collection<T>, toString: (T) -> CharSequence): Array<CharSequence> {
    val strings = arrayListOf<CharSequence>()
    items.forEach { strings.add(toString.invoke(it)) }
    return strings.toTypedArray()
}