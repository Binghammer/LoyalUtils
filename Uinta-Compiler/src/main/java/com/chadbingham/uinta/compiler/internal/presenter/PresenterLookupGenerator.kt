package com.chadbingham.uinta.compiler.internal.presenter

class PresenterLookupGenerator(private val scope: Scope, private val presenters: List<PresenterDescription>) {

    /*
    private val dependencies: MutableSet<Dependency> = mutableSetOf()

    private val className: String
        get() = "${scope.name}PresenterLookups"

    init {
        presenters.forEach {
            dependencies.addAll(it.dependencies)
        }
    }

    private fun generateMethods(description: PresenterDescription): FunSpec {
        return FunSpec
                .builder(description.methodName)
                .addModifiers(KModifier.PUBLIC)
                .returns(TypeName.get(description.element.asType()))
                .apply {
                    val params = description.dependencies.map { it.name }
                    val fulfilment = params.joinToString { it }

                    val element = description.element
                    val key = "\"${description.className}\""
                    if (description.isRetained) {
                        beginControlFlow("if(${Helper.PRESENTERS}.containsKey($key))")
                        addStatement("return (\$T) ${Helper.PRESENTERS}.get($key)", description.element)
                        endControlFlow()
                    }

                    addStatement("final \$T presenter = new \$T($fulfilment)", element, element)
                    if (description.isRetained) {
                        addStatement("${Helper.PRESENTERS}.put($key, presenter)")
                    }

                    addStatement("return (\$T) presenter", description.element)

                }
                .build()
    }

    private fun generateConstructor(): FunSpec {
        return FunSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
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

        val presenter = ParameterizedTypeName.get(
                ClassName.get(UintaPresenter::class.java),
                WildcardTypeName.subtypeOf(UintaInteractor::class.java))

        val map = ParameterizedTypeName.get(
                ClassName.get(Map::class.java),
                ClassName.get(String::class.java),
                WildcardTypeName.supertypeOf(presenter))

        return TypeSpec
                .classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor)
                .addField(FieldSpec
                        .builder(map, Helper.PRESENTERS, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer(CodeBlock.of("new \$T()", ClassName.get(HashMap::class.java)))
                        .build())
                .apply {
                    dependencies.forEach {
                        addField(TypeName.get(it.mirror), it.name, Modifier.PRIVATE, Modifier.FINAL)
                    }
                }
                .addMethods(methods)
                .build()
    }

    private fun findPackageName(): String {
        var packageName = ""
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

        return packageName
    }

    fun write(filer: Filer) {
        val greeterClass = ClassName("", "Greeter")
        val file = FileSpec.builder("", "HelloWorld")
                .addType(TypeSpec.classBuilder("Greeter")
                        .primaryConstructor(FunSpec.constructorBuilder()
                                .addParameter("name", String::class)
                                .build())
                        .addProperty(PropertySpec.builder("name", String::class)
                                .initializer("name")
                                .build())
                        .addFunction(FunSpec.builder("greet")
                                .addStatement("println(%S)", "Hello, \$name")
                                .build())
                        .build())
                .addFunction(FunSpec.builder("main")
                        .addParameter("args", String::class, KModifier.VARARG)
                        .addStatement("%T(args[0]).greet()", greeterClass)
                        .build())
                .build()


        val packageName = findPackageName()

        val source = filer.createResource(, "$packageName.$className")
        val writer = source.openWriter()
        file.writeTo(source)
        writer.flush()
        writer.close()
    }

    */
}
