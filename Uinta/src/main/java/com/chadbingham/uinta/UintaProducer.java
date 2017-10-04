package com.chadbingham.uinta;

import io.reactivex.Single;

public interface UintaProducer<I extends UintaInteractor, T extends UintaPresenter<I>> {
	Single<T> get();
}