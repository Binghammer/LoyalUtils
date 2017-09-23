package com.chadbingham.loyautils.rx;

import java.util.concurrent.Executors;

import io.reactivex.CompletableTransformer;
import io.reactivex.FlowableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ScheduleFirebase {

	public static final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());

	public static <T> FlowableTransformer<T, T> flowable() {
		return o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
	}

	public static <T> ObservableTransformer<T, T> observable() {
		return o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
	}

	public static <T> SingleTransformer<T, T> single() {
		return o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
	}

	public static <T> MaybeTransformer<T, T> maybe() {
		return o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
	}

	public static CompletableTransformer completable() {
		return o -> o.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
	}
}
