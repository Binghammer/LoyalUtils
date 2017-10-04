package com.chadbingham.uinta;

public abstract class UintaPresenter<I extends UintaInteractor> {

	protected I interactor;

	public abstract void start();

	public void attach(I interactor) {
		this.interactor = interactor;
		onAttached();
	}

	public void detach() {
		onDetached();
		interactor = null;
	}

	protected abstract void onDetached();

	protected abstract void onAttached();
}
