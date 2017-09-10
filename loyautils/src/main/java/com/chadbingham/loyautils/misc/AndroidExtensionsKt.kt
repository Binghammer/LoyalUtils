package com.chadbingham.loyautils.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat

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

fun Context.drawable(drawableId: Int): Drawable {
    return ContextCompat.getDrawable(this, drawableId)
}

fun Context.bitmap(drawableId: Int): Bitmap {
    val options = BitmapFactory.Options()
    options.inScaled = false

    var drawable = ContextCompat.getDrawable(this, drawableId)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = DrawableCompat.wrap(drawable).mutate()
    }

    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

fun Context.color(id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.dimen(id: Int): Float {
    return resources.getDimension(id)
}