package com.chadbingham.uinta.compiler.internal.presenter

import com.chadbingham.uinta.Uinta
import com.chadbingham.uinta.UintaInteractor
import com.chadbingham.uinta.UintaPresenter
import com.chadbingham.uinta.annotations.PresenterConstructor
import com.chadbingham.uinta.annotations.PresenterScope
import com.chadbingham.uinta.annotations.PresenterSpec
import com.chadbingham.uinta.compiler.Dependencies
import com.chadbingham.uinta.compiler.internal.presenter.Helper.PRESENTERS
import com.chadbingham.uinta.compiler.processor.ErrorReporter
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import com.sun.tools.javac.code.Symbol
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.*
import javax.lang.model.element.Modifier.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class PresenterProcessingStep(
        private val filer: Filer,
        private val types: Types,
        private val messager: Messager,
        private val elements: Elements) : ProcessingStep {

    private val presenters: PresentersSet = PresentersSet()

    private val uintaMirror = elements.getTypeElement(UintaPresenter::class.java.name)
    private val uintaClassName = uintaMirror.qualifiedName
    private val undefinedScope = Scope("")

    override fun annotations(): MutableSet<out Class<out Annotation>> {
        return ImmutableSet.of(Dependencies.PRESENTER_SPEC)
    }

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<Element> {
        for (element in elementsByAnnotation.values()) {
            parsePresenter(element)?.let { presenters.add(it) }
        }

        val factories = presenters.generateFactories(elements)

        factories.forEach { it.write(filer) }

        return ImmutableSet.of()
    }

    private fun isValidPresenter(element: TypeElement): Boolean {
        return try {
            if (element.kind != ElementKind.CLASS) {
                logDebug("isValidPresenter: $element FALSE")
                false
            } else if (element.qualifiedName == uintaClassName) {
                logDebug("isValidPresenter: $element FOUND")
                true

            } else {
                val superclass = element.superclass
                if (superclass != null) {
                    logDebug("isValidPresenter: $element checking superclass")
                    isValidPresenter(MoreTypes.asTypeElement(superclass))
                } else {
                    logDebug("isValidPresenter: $element TRUE")
                    false
                }
            }
        } catch (e: Exception) {
            logDebug("isValidPresenter: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun parsePresenter(element: Element): PresenterDescription? {
        printElement(element)

        val reporter = ErrorReporter(element, messager)
        val simpleName = element.simpleName

        val annotationMirror = MoreElements.getAnnotationMirror(element, PresenterSpec::class.java).get()
        val annotation = element.getAnnotation(PresenterSpec::class.java)
        logDebug("$simpleName: Annotation: $annotation")

        val scopeAnnotations = element.getAnnotationsByType(PresenterScope::class.java)
        val scopes = mutableListOf<Scope>()
        if (scopeAnnotations.isNotEmpty()) {
            scopes.addAll(scopeAnnotations.map { Scope(it.name) })
        }

        if (scopes.isEmpty()) scopes.add(undefinedScope)

        val isSingleton = annotation.singleton
        val isRetained = annotation.retain

        if (element !is TypeElement) {
            reporter.reportError("Bad Element")
            return null
        }

        if (!isValidPresenter(element)) {
            reporter.reportError("must inherit from $uintaClassName")
        }

        val name = ClassName.get(element)

        val dependencies = mutableListOf<Dependency>()

        val ctrs = element.enclosedElements.filter { it.kind == ElementKind.CONSTRUCTOR }
        val constructor = when {
            ctrs.size == 1 -> ctrs[0]

            ctrs.isEmpty() -> {
                reporter.reportError("Must provide an empty constructor")
                null
            }

            else -> {
                val annotated = ctrs.filter { it.getAnnotation(PresenterConstructor::class.java) != null }
                when {
                    annotated.size == 1 -> annotated[0]
                    else -> {
                        reporter.reportError("If multiple constructors are provided, " +
                                "you must annotate one and only one with @PresenterConstructor")
                        null
                    }
                }
            }
        }

        if (constructor != null) {
            val types = (constructor as ExecutableElement).parameters
            val names = (constructor as Symbol.MethodSymbol).savedParameterNames
            check(names.size == types.size, { "Unexpected error. types.size=${types.size} and names.size=${names.size}" })
            (0 until types.size).mapTo(dependencies) {
                Dependency(types[it].asType(), names[it].toString())
            }

        } else {
            reporter.reportError("No constructor found")
        }

        return if (reporter.hasError) {
            null
        } else {
            PresenterDescription(
                    annotation = annotationMirror,
                    className = name,
                    element = element,
                    scopes = scopes,
                    dependencies = dependencies,
                    isRetained = isRetained,
                    isSingleton = isSingleton,
                    constructor = constructor!!)
        }
    }

    private fun printElement(element: Element) {
        logDebug("${element.simpleName}: " + StringBuilder().apply {
            fun br() = append("\n\t")

            append("ELEMENT: {")
            br()
            append(element)
            br().append("kind: ").append(element.kind)

            if (element is TypeElement) {
                br().append("qualified: ").append(element.qualifiedName)
                br().append("nestingKind: ").append(element.nestingKind)
                br().append("interfaces: ").append(element.interfaces)
                br().append("superclass: ").append(element.superclass)
                br().append("enclosed: ").append(element.enclosedElements)
                br().append("enclosing: ").append(element.enclosingElement)
                br().append("typeParameters: ").append(element.typeParameters)
                br().append("kind: ").append(element.kind)
            }

            br().append("enclosed: ").append(element.enclosedElements)
            br().append("enclosing: ").append(element.enclosingElement)
            append("\n").append("}")

        }.toString())
    }

    private fun logDebug(message: String) {
        if (Uinta.debugLogging) {
            System.out.println(message)
        }
    }
}

data class PresenterDescription(
        val annotation: AnnotationMirror,
        var className: ClassName,
        var element: Element,
        val scopes: List<Scope>,
        val dependencies: List<Dependency>,
        val isSingleton: Boolean,
        val isRetained: Boolean,
        val constructor: Element) {
    val methodName: String
        get() = "get${className.simpleName()}"
}

data class Scope(val name: String)

data class Dependency(val mirror: TypeMirror, val name: String)

class PresentersSet {

    private val presenters: Multimap<Scope, PresenterDescription> = HashMultimap.create()
    private val dependencies: MutableSet<Dependency> = mutableSetOf()

    fun add(description: PresenterDescription) {
        for (scope in description.scopes) {
            presenters.put(scope, description)
        }

        dependencies.addAll(description.dependencies)
    }

    fun generateFactories(elements: Elements): List<PresenterFactory> {
        return presenters.keys().map {
            PresenterFactory(it, presenters.get(it).toList())
        }
    }
}

object Helper {
    const val PRESENTERS = "presenters"

    fun createHashMap(): TypeName {
        val interactorWildcard = WildcardTypeName.subtypeOf(UintaInteractor::class.java)
        val uintaPresenterType: ClassName = ClassName.get(UintaPresenter::class.java)

        val uintaParameterized = ParameterizedTypeName.get(uintaPresenterType, interactorWildcard)
        val uintaWildcard: TypeName = WildcardTypeName.supertypeOf(uintaParameterized)

        val string: ClassName = ClassName.get(String::class.java)
        val mapTypeName: ClassName = ClassName.get(Map::class.java)
        return ParameterizedTypeName.get(mapTypeName, string, uintaWildcard)
    }
}

class PresenterFactory(private val scope: Scope, private val presenters: List<PresenterDescription>) {
    private var map = Helper.createHashMap()

    private var packageName: String = ""

    private val mapFieldSpec = FieldSpec.builder(map, PRESENTERS, PRIVATE, FINAL)
            .initializer(CodeBlock.of("new \$T()", ClassName.get(HashMap::class.java)))
            .build()!!

    private val dependencies: MutableSet<Dependency> = mutableSetOf()

    private val className: String
        get() = "${scope.name}PresenterFactory"

    init {
        presenters.forEach {
            dependencies.addAll(it.dependencies)
        }
    }

    private fun generateMethods(description: PresenterDescription): MethodSpec {
        return MethodSpec
                .methodBuilder(description.methodName)
                .addModifiers(PUBLIC)
                .returns(TypeName.get(description.element.asType()))
                .apply {
                    val params = description.dependencies.map { it.name }
                    val fulfilment = params.joinToString { it }

                    val element = description.element
                    val key = "\"${description.className}\""
                    if (description.isRetained) {
                        beginControlFlow("if($PRESENTERS.containsKey($key))")
                        addStatement("return (\$T) $PRESENTERS.get($key)", description.element)
                        endControlFlow()
                    }

                    addStatement("final \$T presenter = new \$T($fulfilment)", element, element)
                    if (description.isRetained) {
                        addStatement("$PRESENTERS.put($key, presenter)")
                    }

                    addStatement("return (\$T) presenter", description.element)

                }
                .build()
    }

    private fun generateConstructor(): MethodSpec {
        return MethodSpec
                .constructorBuilder()
                .addModifiers(PUBLIC)
                .apply {
                    dependencies.forEach {
                        addParameter(TypeName.get(it.mirror), it.name)
                        addStatement("this.${it.name} = ${it.name}")
                    }
                }
                .build()
    }

    private fun generateClass(): TypeSpec {
        val constructor = generateConstructor()
        val methods = presenters.map { generateMethods(it) }
        return TypeSpec
                .classBuilder(className)
                .addModifiers(PUBLIC)
                .addMethod(constructor)
                .addField(mapFieldSpec)
                .apply {
                    dependencies.forEach {
                        addField(TypeName.get(it.mirror), it.name, PRIVATE, FINAL)
                    }
                }
                .addMethods(methods)
                .build()
    }

    private fun findPackageName() {
        presenters.forEach {
            val packageElement = it.element.enclosingElement
            val pkg: String = packageElement.toString()
            packageName = if (packageName.isBlank()) {
                pkg
            } else {
                val current = packageName.split(".")
                val pkgSplit = pkg.split(".")

                val result = (0..current.size)
                        .filter { current[it] == pkgSplit[it] }
                        .map { current[it] }

                result.joinToString(separator = ".")
            }
        }
    }

    fun write(filer: Filer) {
        findPackageName()
        val typeSpec = generateClass()
        val javaFile = JavaFile.builder(packageName, typeSpec).build()
        val source = filer.createSourceFile("$packageName.$className")
        val writer = source.openWriter()
        javaFile.writeTo(writer)
        writer.flush()
        writer.close()
    }
}