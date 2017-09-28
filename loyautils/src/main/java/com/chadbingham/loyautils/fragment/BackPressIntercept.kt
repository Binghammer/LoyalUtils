package com.chadbingham.loyautils.fragment

interface BackPressIntercept {
    /**
     * @return true if this fragment intercepted the back press
     */
    fun onBackPress(): Boolean
}