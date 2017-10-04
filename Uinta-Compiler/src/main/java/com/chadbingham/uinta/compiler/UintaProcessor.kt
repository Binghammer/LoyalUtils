package com.chadbingham.uinta.compiler

import com.chadbingham.uinta.compiler.internal.interactor.InteractorProcessingStep
import com.chadbingham.uinta.compiler.internal.presenter.PresenterProcessingStep
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.ImmutableList
import com.google.googlejavaformat.java.filer.FormattingFiler
import javax.lang.model.SourceVersion

class UintaProcessor: BasicAnnotationProcessor() {

    override fun initSteps(): Iterable<BasicAnnotationProcessor.ProcessingStep> {
        println("******** RUNNING ********")
        val filer = FormattingFiler(processingEnv.filer)
        val messager = processingEnv.messager
        val elements = processingEnv.elementUtils
        val types = processingEnv.typeUtils

        return ImmutableList.of(
                InteractorProcessingStep(filer, types, messager, elements),
                PresenterProcessingStep(filer, types, messager, elements))
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
}