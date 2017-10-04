package com.chadbingham.uinta.compiler.internal.interactor

import com.chadbingham.uinta.compiler.ClassType
import com.chadbingham.uinta.compiler.Dependencies
import com.chadbingham.uinta.compiler.Dependencies.GENERATED_PACKAGE
import com.chadbingham.uinta.compiler.Dependencies.VAL_ACTIVITY
import com.chadbingham.uinta.compiler.Dependencies.VAL_CONTAINER
import com.chadbingham.uinta.compiler.Dependencies.VAL_CONTEXT
import com.chadbingham.uinta.compiler.processor.ErrorReporter
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import com.google.auto.common.MoreElements
import com.google.common.base.CaseFormat
import com.google.common.collect.ImmutableSet
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.*
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class InteractorProcessingStep(
        private val filer: Filer,
        private val types: Types,
        private val messager: Messager,
        private val elements: Elements) : ProcessingStep {

    private val interactors: InteractorSet = InteractorSet(elements)

    override fun annotations(): Set<Class<out Annotation>> {
        return ImmutableSet.of(Dependencies.INTERACTOR_SPEC)
    }

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        for (element in elementsByAnnotation.values()) {
            parseInteractor(element)?.let { interactors.add(it) }
        }

        interactors.write(filer)
        return ImmutableSet.of()
    }

    private fun parseInteractor(element: Element): InteractorDescription? {
        val reporter = ErrorReporter(element, messager)

        val annotatedClassName = TypeName.get(element.asType()) as ClassName

        if (element.kind != ElementKind.INTERFACE) {
            reporter.reportError("InteractorSpec must be an interface: " + element.kind)
        }

        val typeElement = element as TypeElement
        if (!typeElement.modifiers.contains(PUBLIC)) {
            reporter.reportError("Class must be public.")
        }

        val annotationMirror = MoreElements.getAnnotationMirror(element, Dependencies.INTERACTOR_SPEC).get()
        reporter.annotation = annotationMirror
        val annotationValue = AnnotationMirrors.getAnnotationValue(annotationMirror, "implementationClass")

        val implementationTypeElement: TypeMirror = annotationValue.value as TypeMirror
        val implementationClassName = TypeName.get(implementationTypeElement) as ClassName

        val type = InteractorType.values().firstOrNull {
            types.isAssignable(implementationTypeElement, it.classType.typeMirror(elements))
        }

        if (type == null)
            reporter.reportError("No type found")

        return if (reporter.hasError) {
            null
        } else {
            InteractorDescription(
                    annotatedTypeElement = element,
                    annotatedClassName = annotatedClassName,
                    implementationClassName = implementationClassName,
                    implementationTypeElement = implementationTypeElement,
                    type = type)
        }
    }
}

enum class InteractorType(val classType: ClassType) {
    FRAGMENT(ClassType.FRAGMENT),
    VIEW(ClassType.VIEW)
}

data class InteractorDescription(
        val annotatedTypeElement: TypeElement,
        var annotatedClassName: ClassName,
        var implementationClassName: ClassName,
        var implementationTypeElement: TypeMirror,
        var type: InteractorType?) {

    val needsActivity: Boolean
        get() = type == InteractorType.FRAGMENT

    val tag: String
        get() = "\"${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, annotatedClassName.simpleName())}\""

    val returnType: TypeName
        get() = TypeName.get(annotatedTypeElement.asType())

    val methodName
        get() = "get" + annotatedClassName.simpleName()
}

class InteractorSet(private val elements: Elements) {

    private val interactors: MutableList<InteractorDescription> = mutableListOf()
    private var needsActivity = false

    private var context = VAL_CONTEXT
    private val container = VAL_CONTAINER

    fun add(description: InteractorDescription) {
        if (description.needsActivity) needsActivity = true
        interactors.add(description)
    }

    fun write(filer: Filer) {
        val typeSpec = generateClass()
        val javaFile = JavaFile.builder(Dependencies.GENERATED_PACKAGE, typeSpec).build()
        val source = filer.createSourceFile("$GENERATED_PACKAGE.Interactors")
        val writer = source.openWriter()
        javaFile.writeTo(writer)
        writer.flush()
        writer.close()
    }

    private fun generateClass(): TypeSpec {
        val constructor = generateConstructor()
        return TypeSpec
                .classBuilder("Interactors")
                .addModifiers(PUBLIC)
                .addMethod(constructor)
                .apply {
                    if (needsActivity) {
                        addField(ClassType.ACTIVITY.className, context, PRIVATE, FINAL)
                        addField(TypeName.INT, container, PRIVATE, FINAL)
                    } else {
                        addField(ClassType.CONTEXT.className, context, PRIVATE, FINAL)
                    }
                }
                .addMethods(interactors.map { generateMethod(it) })
                .build()
    }

    private fun generateConstructor(): MethodSpec {
        return MethodSpec
                .constructorBuilder()
                .addModifiers(PUBLIC)
                .apply {
                    if (needsActivity) {
                        context = VAL_ACTIVITY
                        addParameter(ClassType.ACTIVITY.className, context)
                        addParameter(ParameterSpec
                                .builder(TypeName.INT, VAL_CONTAINER)
                                .addAnnotation(Dependencies.LayoutResAnnotationSpec)
                                .build())

                        addStatement("this.$context = $context")
                        addStatement("this.$container = $container")

                    } else {
                        context = VAL_CONTEXT
                        addParameter(ClassType.CONTEXT.className, context)
                        addStatement("this.$context = $context")
                    }
                }
                .build()
    }

    private fun generateMethod(description: InteractorDescription): MethodSpec {
        return MethodSpec
                .methodBuilder(description.methodName)
                .addModifiers(PUBLIC)
                .returns(description.returnType)
                .apply {
                    when (description.type!!) {
                        InteractorType.FRAGMENT -> {
                            val tag = description.tag
                            addStatement("final  \$T impl = new \$T()",
                                    description.annotatedTypeElement,
                                    description.implementationTypeElement)
                            addCode("activity.getSupportFragmentManager()\n" +
                                    ".beginTransaction()\n" +
                                    ".replace(container, (\$T) impl, $tag)\n" +
                                    ".addToBackStack($tag)\n" +
                                    ".commit();\n", Dependencies.fragmentTypeElement(elements))
                            addStatement("return impl")
                        }

                        InteractorType.VIEW -> {
                            addStatement("return new ${description.implementationClassName}(context)")
                        }
                    }
                }
                .build()
    }
}