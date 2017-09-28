package com.chadbingham.loyautils.activity

import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.chadbingham.loyautils.R
import com.chadbingham.loyautils.fragment.BackPressIntercept
import timber.log.Timber

abstract class SupportBaseActivity : AppCompatActivity() {

    @IdRes
    protected open var container: Int = R.id.container

    protected open var logDebug: Boolean = false

    protected open fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(container)
    }

    override fun onBackPressed() {
        val frag = getCurrentFragment()
        if (frag != null && frag is BackPressIntercept) {
            if (frag.onBackPress()) {
                logDebug("Fragment intercepted back press")
                return
            } else {
                logDebug("Fragment did not intercept back press")
            }

        } else {
            logDebug("Fragment does not implement ${BackPressIntercept::class.java.simpleName}")
        }

        super.onBackPressed()
    }

    private fun logDebug(message: String) {
        if (logDebug)
            Timber.d(message)
    }

}