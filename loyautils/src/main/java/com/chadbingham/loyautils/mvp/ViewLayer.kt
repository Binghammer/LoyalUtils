package com.chadbingham.loyautils.mvp

interface ViewLayer {
    fun showLoading()
    fun hideLoading()
    fun onError(error: String?)
    fun showAlert(message: String, title: String? = null)
    fun kill()
}