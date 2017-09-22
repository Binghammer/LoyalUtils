package com.chadbingham.loyautils.view

import android.app.AlertDialog
import android.content.Context
import android.support.annotation.StringRes
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.InputType.TYPE_NULL
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.chadbingham.loyautils.R
import com.chadbingham.loyautils.misc.Selector
import io.reactivex.Maybe
import io.reactivex.Observable
import java.util.*

class DynamicTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle), View.OnTouchListener {

    private val editText: TextInputEditText
    private val layout: TextInputLayout

    private var adapter: Adapter<*>? = null
    private var dialog: AlertDialog? = null

    private val touchSlop: Int
    private var brokenSlop: Boolean = false
    private var xDown: Float = 0.toFloat()
    private var yDown: Float = 0.toFloat()

    private var dialogIntercept: DialogIntercept? = null

    init {
        View.inflate(context, R.layout.view_dynamic_text, this)
        editText = findViewById(R.id.dynamicText)
        layout = findViewById(R.id.dynamicLayout)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.DynamicTextView)
        val hint = ta.getString(R.styleable.DynamicTextView_android_hint)
        val inputType = ta.getInt(R.styleable.DynamicTextView_android_inputType, EditorInfo.TYPE_CLASS_TEXT)
        val maxLines = ta.getInt(R.styleable.DynamicTextView_android_maxLines, 1)
        val minLines = ta.getInt(R.styleable.DynamicTextView_android_minLines, 1)
        val lines = ta.getInt(R.styleable.DynamicTextView_android_lines, 1)
        val textSizeDefault = context.resources.getDimension(R.dimen.text_size_default)
        val size = ta.getDimension(R.styleable.DynamicTextView_android_textSize, textSizeDefault)
        ta.recycle()

        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        setHint(hint)
        setInputType(inputType)
        setMaxLines(maxLines)
        setMinLines(minLines)
        setLines(lines)

        val vc = ViewConfiguration.get(context)
        touchSlop = vc.scaledTouchSlop

        if (isInEditMode && (layout.hint == null || layout.hint!!.isEmpty())) {
            editText.setText("Dynamic TextView")
        }
    }

    fun getTextChangeEvents(): Observable<TextChangedEvent> {
        return Observable.create { e ->
            val watcher = object : TextWatcherAdapter() {
                override fun afterTextChanged(s: Editable) {
                    e.onNext(TextChangedEvent(getText()))
                }
            }

            e.setCancellable { editText.removeTextChangedListener(watcher) }
            editText.addTextChangedListener(watcher)
        }
    }

    fun getTextChanges(): Observable<String> {
        return getTextChangeEvents().map { event -> event.text }
    }

    fun clearError() {
        layout.error = null
        layout.isErrorEnabled = false
    }

    fun setError(errorId: Int) {
        layout.error = context.getString(errorId)
    }

    fun setHint(@StringRes id: Int) {
        setHint(context.getString(id))
    }

    fun setHint(hint: String) {
        layout.hint = hint
        dialog?.apply { setTitle(hint) }
    }

    fun setMaxLines(maxLines: Int) {
        editText.maxLines = maxLines
    }

    fun setMinLines(minLines: Int) {
        editText.minLines = minLines
    }

    fun setLines(lines: Int) {
        editText.setLines(lines)
    }

    fun setInputType(type: Int) {
        editText.inputType = type
    }

    fun setText(text: String?) {
        editText.setText(text)
        clearError()
    }

    fun setText(id: Int) {
        editText.setText(id)
        clearError()
    }

    fun setTextSize(size: Int) {
        editText.textSize = size.toFloat()
    }

    fun setDialogIntercept(dialogIntercept: DialogIntercept) {
        setHasDialog()
        this.dialogIntercept = dialogIntercept
    }

    fun setAdapter(adapter: Adapter<*>) {
        setHasDialog()
        this.adapter = adapter
        adapter.view = this
    }

    private fun setHasDialog() {
        adapter = null
        dialog = null
        dialogIntercept = null
        editText.inputType = TYPE_NULL
        editText.isCursorVisible = false
        editText.setOnTouchListener(this)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            ACTION_UP -> {
                if (!brokenSlop)
                    showDialog()
                brokenSlop = false
                yDown = 0f
                xDown = yDown
            }

            ACTION_CANCEL -> {
                brokenSlop = false
                yDown = 0f
                xDown = yDown
            }

            ACTION_DOWN -> {
                xDown = x
                yDown = y
            }

            MotionEvent.ACTION_MOVE -> {
                val xDiff = Math.abs(xDown - event.x)
                val yDiff = Math.abs(yDown - event.y)

                if (brokenSlop || xDiff >= touchSlop || yDiff >= touchSlop) {
                    brokenSlop = true
                }
            }
        }
        return true
    }

    private fun showDialog() {
        //in case it is showing hide the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)

        if (dialogIntercept != null) {
            dialogIntercept?.onShowDialog(getText())?.subscribe({ s -> setText(s) })
            return
        }

        if (dialog == null) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(layout.hint)
                    .setItems(adapter?.listableItems, { _, index -> setSelected(index) })
                    .create()

            dialog?.window!!.attributes.windowAnimations = R.style.AlertDialogAnimations
        }

        dialog?.let {
            if (!TextUtils.isEmpty(layout.hint))
                it.setTitle(layout.hint!!.toString().replace("*", ""))

            if (!it.isShowing()) {
                it.show()
            }
        }
    }

    fun getText(): String {
        return if (editText.text == null) "" else editText.text.toString()
    }

    private fun setSelected(index: Int) {
        editText.setText(adapter?.listableItems!![index])
        adapter?.onItemSelected(index)
        clearError()
    }

    class Adapter<T> {

        internal var listableItems: Array<String>? = null
        private var onItemSelectedListener: OnItemSelectedListener<T>? = null
        private var items: List<T>? = null
        private var stringSelector: Selector<T, String>? = null

        private var selectedIndex: Int = 0

        internal var view: DynamicTextView? = null

        constructor() {
            items = ArrayList()
            stringSelector = object : Selector<T, String> {
                override fun select(t: T): String {
                    return t.toString()
                }
            }
        }

        constructor(items: List<T>) {
            setItems(items)
        }

        fun setStringSelector(stringSelector: Selector<T, String>) {
            this.stringSelector = stringSelector
        }

        fun setItems(items: List<T>) {
            this.items = items
            listableItems = arrayOf()
            listableItems?.let {
                for (i in items.indices) {
                    it[i] = stringSelector!!.select(items[i])
                }
            }
        }

        fun setOnItemSelectedListener(onItemSelectedListener: OnItemSelectedListener<T>) {
            this.onItemSelectedListener = onItemSelectedListener
        }

        fun setSelectedIndex(selectedIndex: Int) {
            this.selectedIndex = selectedIndex
            view?.let {
                if (selectedIndex >= 0) {
                    it.editText.setText(listableItems!![selectedIndex])
                }
            }
        }

        internal fun onItemSelected(selectedIndex: Int) {
            this.selectedIndex = selectedIndex
            if (onItemSelectedListener != null) {
                selectedItem?.let {
                    onItemSelectedListener!!.onItemSelectedListener(it, this.selectedIndex)
                }
            }
        }

        fun getSelectedIndex(): Int {
            return selectedIndex
        }

        var selectedItem: T?
            get() = items!![selectedIndex]
            set(t) {
                if (t == null) {
                    setSelectedIndex(-1)
                }
                setSelectedIndex(items!!.indexOf(t))
            }
    }

    class TextChangedEvent(val text: String)

    interface DialogIntercept {
        fun onShowDialog(currentText: String): Maybe<String>
    }

    interface OnItemSelectedListener<T> {
        fun onItemSelectedListener(item: T, selectedIndex: Int)
    }
}