package com.chadbingham.loyalutilsapp

import com.chadbingham.uinta.UintaInteractor
import com.chadbingham.uinta.annotations.InteractorSpec

@InteractorSpec(implementationClass = InteractorTestImpl::class)
interface InteractorTest: UintaInteractor