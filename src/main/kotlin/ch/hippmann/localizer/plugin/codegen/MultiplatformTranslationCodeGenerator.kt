package ch.hippmann.localizer.plugin.codegen

import ch.hippmann.localizer.plugin.model.Language
import ch.hippmann.localizer.plugin.model.Translation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import java.io.File
import java.util.*

internal object MultiplatformTranslationCodeGenerator {
    fun generateTranslationFile(
        languageToTranslationsMap: Map<Language, List<Translation>>,
        baseLanguage: Language,
        srcRoot: File,
        packagePath: String
    ) {
        val translationFileSpec = FileSpec
            .builder(packagePath, "TR")
            .addFileComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")

        val trObjectSpec = TypeSpec
            .objectBuilder("TR")
            .addAnnotation(ClassName("kotlin.native.concurrent", "ThreadLocal"))

        requireNotNull(languageToTranslationsMap[baseLanguage]).forEach { translation ->
            val keyPropertySpec = PropertySpec
                .builder(translation.key, String::class)
                .initializer("%S", translation.key)
                .addModifiers(KModifier.CONST)
                .build()

            trObjectSpec.addProperty(keyPropertySpec)
        }

        val invokeFunSpec = FunSpec
            .builder("invoke")
            .addModifiers(KModifier.OPERATOR)
            .returns(String::class)
            .addParameter("key", String::class)
            .addParameter(
                ParameterSpec
                    .builder("formatArgs", Any::class, KModifier.VARARG)
                    .defaultValue("arrayOf()")
                    .build()
            )
            .beginControlFlow(
                "return·when(%T.getLanguageEnum())",
                ClassName(
                    packagePath,
                    "LocalisationProvider"
                )
            )

        languageToTranslationsMap.keys.forEach { language ->
            invokeFunSpec.addStatement(
                "%T.${language.uppercase(Locale.ENGLISH)}·->·$language[key]·?:·throw·IllegalArgumentException(%P)",
                ClassName(packagePath, "Language"),
                "No ${language.uppercase(Locale.ENGLISH)} string res for key: \$key"
            )
        }

        invokeFunSpec
            .endControlFlow()
            .beginControlFlow(".let·{·translation·->")
            .beginControlFlow("if·(formatArgs.isNotEmpty())")
            .addStatement("var·offset·=·0")
            .addStatement("var·finalTranslation·=·translation")
            .addStatement("%S.toRegex().findAll(translation)", "(?<!\\\\)%[sd]")
            .beginControlFlow(".forEach")
            .addStatement("finalTranslation·=·finalTranslation.replace(it.value,·formatArgs.getOrNull(offset)?.toString()·?:·\"<No·formatArg·for·index·\$offset>\")")
            .addStatement("offset++")
            .endControlFlow()
            .addStatement("finalTranslation")
            .endControlFlow()
            .beginControlFlow("else")
            .addStatement("translation")
            .endControlFlow()
            .endControlFlow()


        trObjectSpec.addFunction(invokeFunSpec.build())
        translationFileSpec.addType(trObjectSpec.build())

        languageToTranslationsMap.forEach { (language, translations) ->
            val initStatement = CodeBlock
                .builder()
                .addStatement("mapOf(")

            translations.forEach { translation ->
                initStatement.addStatement(
                    "%M to %S,",
                    MemberName(
                        "$packagePath.TR",
                        translation.key
                    ),
                    translation
                )
            }

            initStatement.addStatement(")")

            val languageTranslationsMap = PropertySpec
                .builder(
                    language,
                    Map::class.asClassName().parameterizedBy(
                        String::class.asClassName(),
                        String::class.asClassName()
                    )
                )
                .initializer(initStatement.build())
                .addAnnotation(ClassName("kotlin.native.concurrent", "ThreadLocal"))
                .addModifiers(KModifier.PRIVATE)
                .build()

            translationFileSpec.addProperty(languageTranslationsMap)
        }

        translationFileSpec.build().writeTo(srcRoot)
    }
}
