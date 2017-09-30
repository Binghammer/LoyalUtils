package com.chadbingham.loyautils.view

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicBlur

/**
 * Be sure to add these to your defaultConfig in build.gradle:
 *
 *         renderscriptTargetApi 19
 *         renderscriptSupportModeEnabled true
 *
 *
 */
class BlurUtil(context: Context) {

    private val renderScript = RenderScript.create(context)!!

    fun blur(bitmap: Bitmap, radius: Float = 8F) {
        //this will blur the bitmap with a radius of 8 and save it in bitmap
        val input = Allocation.createFromBitmap(renderScript, bitmap)
        val output = Allocation.createTyped(renderScript, input.type)
        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)
    }
}