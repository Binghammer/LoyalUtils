package com.chadbingham.loyautils.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import com.chadbingham.loyautils.R
import com.chadbingham.loyautils.misc.onError
import com.chadbingham.loyautils.rx.Event
import com.chadbingham.loyautils.view.DynamicTextView
import com.chadbingham.loyautils.view.clicks
import com.chadbingham.loyautils.view.hide
import com.chadbingham.loyautils.view.show
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.MaybeSubject

data class EditTextDialog(var requireText: Boolean = false,
                          var allowDelete: Boolean = false,
                          var error: Int = R.string.required,
                          var title: Int = R.string.enter_text,
                          var hint: Int = R.string.enter_text,
                          var inputType: Int = EditorInfo.TYPE_CLASS_TEXT,
                          var text: String? = null) {
    fun show(fragmentManager: FragmentManager): Maybe<Event<String>> {
        val dialog = EditTextActualDialog()
        dialog.title = title
        dialog.requireText = requireText
        dialog.allowDelete = allowDelete
        dialog.error = error
        dialog.hint = hint
        dialog.text = text
        dialog.inputType = inputType
        return dialog.subject.doOnSubscribe({
            dialog.show(fragmentManager, "EditTextDialog")
        })
    }
}

class EditTextActualDialog : DialogFragment() {

    var requireText: Boolean = false
    var allowDelete: Boolean = false
    var error: Int = R.string.required
    var title: Int = R.string.enter_text
    var hint: Int = R.string.enter_text
    var text: String? = null
    var inputType: Int = EditorInfo.TYPE_CLASS_TEXT

    val subject: MaybeSubject<Event<String>> = MaybeSubject.create<Event<String>>()

    private lateinit var dialogEditText: DynamicTextView
    private lateinit var dialogTitle: TextView
    private lateinit var bSubmit: Button
    private lateinit var bDelete: Button
    private lateinit var bCancel: Button

    private val disposables = CompositeDisposable()

    private var postedResults: Boolean = false

    companion object {
        fun instance(title: Int, hint: Int, text: String? = null): Maybe<Event<String>> {
            val dialog = EditTextActualDialog()
            dialog.title = title
            dialog.hint = hint
            dialog.text = text
            return dialog.subject
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater?.inflate(R.layout.dialog_edit_text, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogEditText.setText(text)
        dialogEditText.setInputType(inputType)
        dialogTitle.setText(title)
        dialogEditText.setHint(hint)

        bSubmit.clicks()
                .doOnSubscribe({ disposables.add(it) })
                .filter { checkText() }
                .map { dialogEditText.getText() }
                .subscribe({ post(Event.Added(it)) }, this::onError)

        bCancel.clicks()
                .doOnSubscribe({ disposables.add(it) })
                .subscribe({
                    dialog.dismiss()
                    post(Event.Canceled())
                }, this::onError)

        if (allowDelete) {
            bDelete.show()
            bDelete.clicks()
                    .doOnSubscribe({ disposables.add(it) })
                    .subscribe({ post(Event.Removed()) }, this::onError)

        } else {
            bDelete.hide()
        }
    }

    private fun post(event: Event<String>) {
        subject.onSuccess(event)
        postedResults = true
        dialog.dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        if (!postedResults) {
            subject.onComplete()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        dialog.setOnShowListener {
            dialog.window!!.setLayout((6 * resources.displayMetrics.widthPixels) / 7, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    private fun checkText(): Boolean {
        return if (requireText && dialogEditText.getText().isBlank()) {
            dialogEditText.setError(error)
            false
        } else {
            true
        }
    }
}