package com.chadbingham.loyalutilsapp

import com.chadbingham.uinta.UintaInteractor
import com.chadbingham.uinta.UintaPresenter

abstract class BasePresenter<I : UintaInteractor> : UintaPresenter<I>()
