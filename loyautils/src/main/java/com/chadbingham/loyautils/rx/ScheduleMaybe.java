package com.chadbingham.loyautils.rx;

import io.reactivex.MaybeTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ScheduleMaybe {

	public static <T> MaybeTransformer<T, T> io() {
		return o -> o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}

	public static <T> MaybeTransformer<T, T> computation() {
		return o -> o.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread());
	}
}
