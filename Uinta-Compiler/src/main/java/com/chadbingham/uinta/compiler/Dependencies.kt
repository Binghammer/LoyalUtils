@file:Suppress("UNCHECKED_CAST")

package com.chadbingham.uinta.compiler

import com.chadbingham.uinta.annotations.InteractorSpec
import com.chadbingham.uinta.annotations.PresenterScope
import com.chadbingham.uinta.annotations.PresenterSpec
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements

object Dependencies {
    const val VAL_ACTIVITY = "activity"
    const val VAL_CONTEXT = "context"
    const val VAL_CONTAINER = "container"

    val INTERACTOR_SPEC = InteractorSpec::class.java
    val PRESENTER_SPEC = PresenterSpec::class.java
    val PRESENTER_SCOPE = PresenterScope::class.java

    val LayoutResAnnotationSpec: AnnotationSpec
        get() = AnnotationSpec.builder(ClassType.ID_RES.className).build()

    fun fragmentTypeElement(elements: Elements): TypeMirror = elements.getTypeElement("android.support.v4.app.Fragment").asType()
}

enum class ClassType(private val pkg: String, private val simpleName: String) {
    ACTIVITY("android.support.v4.app", "FragmentActivity"),
    CONTEXT("android.content", "Context"),
    FRAGMENT("android.support.v4.app", "Fragment"),
    VIEW("android.view", "View"),
    LAYOUT_RES("android.support.annotation", "LayoutRes"),
    ID_RES("android.support.annotation", "IdRes"),
    ;

    val className: ClassName
        get() = ClassName.get(pkg, simpleName)

    private val fullyQualifiedName: String
        get() = "$pkg.$simpleName"

    private fun typeElement(elements: Elements): TypeElement = elements.getTypeElement(fullyQualifiedName)
    fun typeMirror(elements: Elements): TypeMirror = typeElement(elements).asType()
}