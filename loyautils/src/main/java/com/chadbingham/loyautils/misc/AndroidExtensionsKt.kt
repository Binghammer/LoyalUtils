package com.chadbingham.loyautils.misc

import android.os.Build

inline fun apiOr(version: Int, action_greater: () -> Unit, action_lower: () -> Unit, inclusive: Boolean = false) {
    fromApi(version, action_greater, inclusive)
    toApi(version, action_lower, inclusive)
}

inline fun toApi(toVersion: Int, action: () -> Unit, inclusive: Boolean = false) {
    if (Build.VERSION.SDK_INT < toVersion || (inclusive && Build.VERSION.SDK_INT == toVersion)) action()
}

inline fun fromApi(fromVersion: Int, action: () -> Unit, inclusive: Boolean = true) {
    if (Build.VERSION.SDK_INT > fromVersion || (inclusive && Build.VERSION.SDK_INT == fromVersion)) action()
}