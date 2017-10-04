package com.chadbingham.uinta.compiler.processor

import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.tools.Diagnostic

class ErrorReporter(
        private val subject: Element,
        private val messager: Messager,
        var annotation: AnnotationMirror? = null) {

    var hasError: Boolean = false

    fun reportError(error: String) {
        hasError = true
        if (annotation == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, error, subject)
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, error, subject, annotation)
        }
    }
}