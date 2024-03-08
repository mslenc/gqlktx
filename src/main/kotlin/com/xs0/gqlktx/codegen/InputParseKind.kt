package com.xs0.gqlktx.codegen

enum class InputParseKind {
    BASELINE, // call InputParsing.[name]()
    MAYBE, // generate parseMaybeOf[name]()
    NOT_NULL, // generate parseNotNullOf[name]()
    ARRAY_OF, // generate parseArrayOf[name]()
    COLLECTION_OF, // generate parseArrayListOf[name]()
    INPUT_OBJECT, // generate parseName()
    ENUM, // generate parseName()
}

data class InputParseCodeGenInfo(
    val kind: InputParseKind,
    val nullable: Boolean = true,
    val funName: String,
    val funReturnType: String,
    val funCreateType: String,
    val outPackageName: String,
    val exprTemplate: String,
    val importsForGen: Set<Pair<String, String>>,
    val importsForUse: Set<Pair<String, String>>,
)

enum class OutputExportKind {
    BASELINE,
    NOT_NULL,
    ARRAY_OF,
    COLLECTION_OF,
    ENUM,
    OBJECT,
    INTERFACE,
}

data class OutputExportCodeGenInfo(
    val kind: OutputExportKind,
    val funName: String,
    val funReturnType: String,
    val funReturnTypeNN: String,
    val funIsSuspending: Boolean,
    val outPackageName: String,
    val exprTemplate: String,
    val importsForGen: Set<Pair<String, String>>,
    val importsForUse: Set<Pair<String, String>>,
)

fun OutputExportCodeGenInfo.buildWrapper(kind: OutputExportKind, prefix: String, hasSubSelections: Boolean, funReturnTypeNN: String, funReturnType: String = funReturnTypeNN + "?"): OutputExportCodeGenInfo {
    val funName = prefix + this.funName
    val finalName: String
    val exprTemplate: String
    if (hasSubSelections) {
        finalName = "executeGQL" + funName
        exprTemplate = finalName + "(SUBSEL, SUBOBJ, SUBPATH, state)"
    } else {
        finalName = "export" + funName
        exprTemplate = finalName + "(VALUE, coercion)"
    }

    return OutputExportCodeGenInfo(
        kind,
        funName,
        funReturnType = funReturnType,
        funReturnTypeNN = funReturnTypeNN,
        funIsSuspending = funIsSuspending,
        outPackageName = outPackageName,
        exprTemplate = exprTemplate,
        importsForGen = importsForUse,
        importsForUse = setOf(outPackageName to finalName)
    )
}