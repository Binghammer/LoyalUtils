package com.chadbingham.loyalutilsapp

import com.chadbingham.uinta.annotations.PresenterScope
import com.chadbingham.uinta.annotations.PresenterSpec

@PresenterSpec
@PresenterScope(name = "activity")
@PresenterScope(name = "context")
class PresenterTest constructor(myActivity: MainActivity, myInteger: Int) : BasePresenter<InteractorTest>() {

    override fun start() {
    }

    override fun onDetached() {
    }

    override fun onAttached() {
    }
}