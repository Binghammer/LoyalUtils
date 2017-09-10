package com.chadbingham.loyautils.view

import android.animation.Animator

open class AnimatorListenerAdapter : Animator.AnimatorListener {
    override fun onAnimationRepeat(animator: Animator?) {}

    override fun onAnimationEnd(animator: Animator?) {}

    override fun onAnimationCancel(animator: Animator?) {}

    override fun onAnimationStart(animator: Animator?) {}
}

open class AnimatorEndListener(protected val function: (animator: Animator?) -> Unit) : AnimatorListenerAdapter() {
    override fun onAnimationEnd(animator: Animator?) {
        function(animator)
    }
}

class AnimatorEndCancelListener(function: (animator: Animator?) -> Unit) : AnimatorEndListener(function) {
    override fun onAnimationCancel(animator: Animator?) {
        function(animator)
    }
}