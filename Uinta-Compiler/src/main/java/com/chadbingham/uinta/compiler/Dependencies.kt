@file:Suppress("UNCHECKED_CAST", "UNUSED")

package com.chadbingham.uinta.compiler

import com.chadbingham.uinta.annotations.InteractorSpec
import com.chadbingham.uinta.annotations.PresenterSpec
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements

object Dependencies {
    const val GENERATED_PACKAGE = "com.chadbingham.uinta.generated"
    const val ANNOTATION_PACKAGE = " com.chadbingham.uinta.annotations"
    private const val SUPPORT_PACKAGE = "android.support.v4.app"

    const val VAL_ACTIVITY = "activity"
    const val VAL_CONTEXT = "context"
    const val VAL_CONTAINER = "container"

    fun typeElement(elements: Elements, fullyQualifiedName: String): TypeElement = elements.getTypeElement(fullyQualifiedName)
    fun typeMirror(elements: Elements, fullyQualifiedName: String): TypeMirror = typeElement(elements, fullyQualifiedName).asType()

    val INTERACTOR_SPEC = InteractorSpec::class.java
    val PRESENTER_SPEC = PresenterSpec::class.java

    val LayoutResAnnotationSpec: AnnotationSpec
        get() = AnnotationSpec.builder(ClassType.LAYOUT_RES.className).build()

    fun viewTypeElement(elements: Elements): TypeMirror = elements.getTypeElement("android.view.View").asType()
    fun fragmentTypeElement(elements: Elements): TypeMirror = elements.getTypeElement("android.support.v4.app.Fragment").asType()
    fun activityTypeElement(elements: Elements): TypeMirror =
            elements.getTypeElement("android.support.v4.app.FragmentActivity").asType()

    fun isSubtypeOfType(element: TypeElement, otherType: String): Boolean {
        var result = false

        if (element.qualifiedName.toString() == otherType) {
            result = true

        } else if (element.asType().kind != TypeKind.DECLARED) {
            result = false

        } else {
            val superType = element.superclass
            System.out.println("Element $element supertype $superType")
            if (isSubtypeOfType(superType, otherType)) {
                result = true
            }

            if (!result) {
                result = element.interfaces.any { isSubtypeOfType(it, otherType) }
            }
        }

        System.out.println("isSubType: $element of $otherType = $result")
        return result
    }

    fun isSubtypeOfType(typeMirror: TypeMirror, otherType: String): Boolean {
        var result = false

        if (isTypeEqual(typeMirror, otherType)) {
            result = true

        } else if (typeMirror.kind != TypeKind.DECLARED) {
            result = false

        } else {
            val declaredType = typeMirror as DeclaredType
            val element = declaredType.asElement() as? TypeElement
            if (element == null) {
                result = false
            } else {
                val superType = element.superclass
                System.out.println("supertype: $superType")
                if (isSubtypeOfType(superType, otherType)) {
                    result = true
                }

                if (!result) {
                    result = element.interfaces.any { isSubtypeOfType(it, otherType) }
                }
            }
        }

        System.out.println("isSubType: $typeMirror of $otherType = $result")
        return result
    }

    fun getTypeArguments(typeMirror: TypeMirror): List<TypeMirror> {
        if (typeMirror.kind != TypeKind.DECLARED) {
            return listOf()
        }

        val declaredType = typeMirror as DeclaredType
        return declaredType.typeArguments
    }

    private fun isTypeEqual(typeMirror: TypeMirror, otherType: String): Boolean {
        return otherType == typeMirror.toString()
    }
}

enum class ClassType(val pkg: String, val simpleName: String) {
    /* Uinta Classes */

    /* Android Classes */
    ACTIVITY("android.support.v4.app", "FragmentActivity"),
    CONTEXT("android.content", "Context"),
    FRAGMENT("android.support.v4.app", "Fragment"),
    VIEW("android.view", "View"),
    LAYOUT_RES("android.support.annotation", "LayoutRes"),
    ;

    val className: ClassName
        get() = ClassName.get(pkg, simpleName)

    val clazz: Class<*>
        get() = Class.forName(fullyQualifiedName)

    val fullyQualifiedName: String
        get() = "$pkg.$simpleName"

    fun typeElement(elements: Elements): TypeElement = elements.getTypeElement(fullyQualifiedName)
    fun typeMirror(elements: Elements): TypeMirror = typeElement(elements).asType()
}

/* to get package info
    PackageElement packageElement = (PackageElement) type.getEnclosingElement();
    packageName = packageElement.getQualifiedName().toString();
 */