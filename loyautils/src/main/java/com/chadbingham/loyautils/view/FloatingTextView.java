package com.chadbingham.loyautils.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.chadbingham.loyautils.R;
import com.chadbingham.loyautils.misc.Selector;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import static android.text.InputType.TYPE_NULL;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class FloatingTextView extends FrameLayout implements View.OnTouchListener {

	private final TextInputEditText editText;
	private final TextInputLayout layout;

	private Adapter adapter;
	private AlertDialog dialog;

	private final int touchSlop;
	private boolean brokenSlop;
	private float xDown;
	private float yDown;

	private DialogIntercept dialogIntercept;

	public FloatingTextView(Context context) {
		this(context, null);
	}

	public FloatingTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FloatingTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		inflate(context, R.layout.view_float_edit_text, this);
		editText = (TextInputEditText) findViewById(R.id.floatText);
		layout = (TextInputLayout) findViewById(R.id.floatLayout);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FloatingTextView);
		final String hint = ta.getString(R.styleable.FloatingTextView_android_hint);
		final int inputType = ta.getInt(R.styleable.FloatingTextView_android_inputType, EditorInfo.TYPE_CLASS_TEXT);
		final int maxLines = ta.getInt(R.styleable.FloatingTextView_android_maxLines, 1);
		final int minLines = ta.getInt(R.styleable.FloatingTextView_android_minLines, 1);
		final int lines = ta.getInt(R.styleable.FloatingTextView_android_lines, 1);
		final float textSizeDefault = context.getResources().getDimension(R.dimen.text_size_default);
		final float size = ta.getDimension(R.styleable.FloatingTextView_android_textSize, textSizeDefault);
		ta.recycle();

		editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
		setHint(hint);
		setInputType(inputType);
		setMaxLines(maxLines);
		setMinLines(minLines);
		setLines(lines);

		final ViewConfiguration vc = ViewConfiguration.get(context);
		touchSlop = vc.getScaledTouchSlop();

		if (isInEditMode() && (layout.getHint() == null || layout.getHint().length() == 0)) {
			editText.setText("Floating Text View");
		}
	}

	public Observable<TextChangedEvent> getTextChangeEvents() {
		return Observable.create(new ObservableOnSubscribe<TextChangedEvent>() {
			@Override
			public void subscribe(final ObservableEmitter<TextChangedEvent> e) throws Exception {
				final TextWatcher watcher = new TextWatcherAdapter() {
					@Override
					public void afterTextChanged(Editable s) {
						e.onNext(new TextChangedEvent(getText()));
					}
				};

				e.setCancellable(new Cancellable() {
					@Override
					public void cancel() throws Exception {
						editText.removeTextChangedListener(watcher);
					}
				});
				editText.addTextChangedListener(watcher);
			}
		});
	}

	public Observable<String> getTextChanges() {
		return getTextChangeEvents().map(new Function<TextChangedEvent, String>() {
			@Override
			public String apply(TextChangedEvent event) throws Exception {
				return event.getText();
			}
		});
	}

	public void clearError() {
		layout.setError(null);
		layout.setErrorEnabled(false);
	}

	public void setError(int errorId) {
		layout.setError(getContext().getString(errorId));
	}

	public void setHint(@StringRes int id) {
		setHint(getContext().getString(id));
	}

	public void setHint(String hint) {
		layout.setHint(hint);
		if (dialog != null)
			dialog.setTitle(hint);
	}

	public void setMaxLines(int maxLines) {
		editText.setMaxLines(maxLines);
	}

	public void setMinLines(int minLines) {
		editText.setMinLines(minLines);
	}

	public void setLines(int lines) {
		editText.setLines(lines);
	}

	public void setInputType(int type) {
		editText.setInputType(type);
	}

	public void setText(String text) {
		editText.setText(text);
		clearError();
	}

	public void setText(int id) {
		editText.setText(id);
		clearError();
	}

	public void setTextSize(int size) {
		editText.setTextSize(size);
	}

	public void setDialogIntercept(DialogIntercept dialogIntercept) {
		setHasDialog();
		this.dialogIntercept = dialogIntercept;
	}

	public void setAdapter(Adapter adapter) {
		setHasDialog();
		this.adapter = adapter;
		adapter.view = this;
	}

	private void setHasDialog() {
		adapter = null;
		dialog = null;
		dialogIntercept = null;
		editText.setInputType(TYPE_NULL);
		editText.setCursorVisible(false);
		editText.setOnTouchListener(this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();

		switch (event.getActionMasked()) {
			case ACTION_UP:
				if (!brokenSlop)
					showDialog();

				//fallthrough

			case ACTION_CANCEL:
				brokenSlop = false;
				xDown = yDown = 0;
				break;

			case ACTION_DOWN:
				xDown = x;
				yDown = y;
				break;

			case MotionEvent.ACTION_MOVE:
				final float xDiff = Math.abs(xDown - event.getX());
				final float yDiff = Math.abs(yDown - event.getY());

				if (brokenSlop || xDiff >= touchSlop || yDiff >= touchSlop) {
					brokenSlop = true;
				}

				break;

		}
		return true;
	}

	private void showDialog() {
		//in case it is showing hide the keyboard
		final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.hideSoftInputFromWindow(getWindowToken(), 0);

		if (dialogIntercept != null) {
			dialogIntercept.onShowDialog(getText()).subscribe(new Consumer<String>() {
				@Override
				public void accept(String s) throws Exception {
					setText(s);
				}
			});
			return;
		}

		if (dialog == null) {
			dialog = new AlertDialog
					.Builder(getContext())
					.setTitle(layout.getHint())
					.setItems(adapter.listableItems, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int index) {
							setSelected(index);
						}
					})
					.create();

			dialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnimations;
		}

		if (!TextUtils.isEmpty(layout.getHint()))
			dialog.setTitle(layout.getHint().toString().replace("*", ""));

		if (!dialog.isShowing()) {
			dialog.show();
		}
	}

	public String getText() {
		return editText.getText() == null ? "" : editText.getText().toString();
	}

	private void setSelected(int index) {
		editText.setText(adapter.listableItems[index]);
		adapter.onItemSelected(index);
		clearError();
	}

	public static class Adapter<T> {

		private String[] listableItems;
		private OnItemSelectedListener<T> onItemSelectedListener;
		private List<T> items;
		private Selector<T, String> stringSelector;

		private int selectedIndex;

		private FloatingTextView view;

		public Adapter() {
			items = new ArrayList<>();
			stringSelector = new Selector<T, String>() {
				@Override
				public String select(T o) {
					return o.toString();
				}
			};
		}

		public Adapter(List<T> items) {
			setItems(items);
		}

		public void setStringSelector(Selector<T, String> stringSelector) {
			this.stringSelector = stringSelector;
		}

		public void setItems(List<T> items) {
			this.items = items;
			this.listableItems = new String[items.size()];

			for (int i = 0; i < items.size(); i++) {
				listableItems[i] = stringSelector.select(items.get(i));
			}
		}

		public void setOnItemSelectedListener(OnItemSelectedListener<T> onItemSelectedListener) {
			this.onItemSelectedListener = onItemSelectedListener;
		}

		public void setSelectedIndex(int selectedIndex) {
			this.selectedIndex = selectedIndex;
			if (view != null && selectedIndex >= 0) {
				view.editText.setText(listableItems[selectedIndex]);
			}
		}

		public void setSelectedItem(T t) {
			if (t == null) {
				setSelectedIndex(-1);
			}
			setSelectedIndex(items.indexOf(t));
		}

		private void onItemSelected(int selectedIndex) {
			this.selectedIndex = selectedIndex;
			if (onItemSelectedListener != null) {
				onItemSelectedListener.onItemSelectedListener(getSelectedItem(), this.selectedIndex);
			}
		}

		public int getSelectedIndex() {
			return selectedIndex;
		}

		public T getSelectedItem() {
			return items.get(selectedIndex);
		}
	}

	public static class TextChangedEvent {
		public final String text;

		public TextChangedEvent(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	public interface DialogIntercept {
		Maybe<String> onShowDialog(String currentText);
	}

	public interface OnItemSelectedListener<T> {
		void onItemSelectedListener(T item, int selectedIndex);
	}
}
