package com.xs0.gqlktx

import kotlin.reflect.*
import kotlin.reflect.full.*

typealias ContextTypes<CTX> = Map<KClass<*>, SyncInvokable<CTX>>

fun <CTX: Any> findContextTypes(ctx: KClass<CTX>): ContextTypes<CTX> {
    val result = HashMap<KClass<*>, SyncInvokable<CTX>>()

    nextProp@
    for (member: KCallable<*> in ctx.members) {
        if (ignoreContextFunc(member, ctx))
            continue

        val invokable: SyncInvokable<CTX> = when(member) {
            is KFunction -> SyncInvokableFunction(
                    member.returnType,
                    member.name,
                    member.returnType.isMarkedNullable,
                    member,
                    member.parameters.isEmpty()
            )

            is KProperty0 -> SyncInvokableUnboundProperty(
                    member.returnType,
                    member.name,
                    member.returnType.isMarkedNullable,
                    member
            )

            is KProperty1<*,*> -> {
                @Suppress("UNCHECKED_CAST")
                member as KProperty1<CTX, *>

                SyncInvokableBoundProperty(
                        member.returnType,
                        member.name,
                        member.returnType.isMarkedNullable,
                        member
                )
            }

            else -> continue@nextProp
        }

        if (result.put(invokable.type.classifier as KClass<*>, invokable) != null) {
            throw IllegalArgumentException("Two context functions return the same type ${invokable.type}")
        }
    }

    return result
}

fun <SCHEMA: Any> findRootMethod(schema: KClass<SCHEMA>,
                                 explicitAnnotation: KClass<out Annotation>,
                                 implicitPropName: String,
                                 implicitFuncName: String): SyncInvokable<SCHEMA>? {

    var implicitRoot: SyncInvokable<SCHEMA>? = null
    var explicitRoot: SyncInvokable<SCHEMA>? = null
    var tooManyImplicitRoots = false

    nextProp@
    for (member: KCallable<*> in schema.members) {
        val ann = member.annotations.firstOrNull { explicitAnnotation.isInstance(it) }

        if (ignoreContextFunc(member, schema)) {
            if (ann != null) {
                throw IllegalStateException("$member is marked with $explicitAnnotation, but is not usable")
            }
            continue@nextProp
        }

        if (ann == null) {
            if (member is KFunction) {
                if (member.name != implicitFuncName)
                    continue@nextProp
            } else {
                if (member.name != implicitPropName)
                    continue@nextProp
            }
        }

        val invokable: SyncInvokable<SCHEMA> = when(member) {
            is KFunction -> SyncInvokableFunction(
                    member.returnType,
                    member.name,
                    member.returnType.isMarkedNullable,
                    member,
                    member.parameters.isEmpty()
            )

            is KProperty0 -> SyncInvokableUnboundProperty(
                    member.returnType,
                    member.name,
                    member.returnType.isMarkedNullable,
                    member
            )

            is KProperty1<*,*> -> {
                @Suppress("UNCHECKED_CAST")
                member as KProperty1<SCHEMA, *>

                SyncInvokableBoundProperty(
                        member.returnType,
                        member.name,
                        member.returnType.isMarkedNullable,
                        member
                )
            }

            else -> continue@nextProp
        }

        if (ann != null) {
            if (explicitRoot != null) {
                explicitRoot = invokable
            } else {
                throw IllegalStateException("More than one method marked with $explicitAnnotation in $schema")
            }
        } else {
            if (implicitRoot == null) {
                implicitRoot = invokable
            } else {
                tooManyImplicitRoots = true
            }
        }
    }

    if (explicitRoot != null)
        return explicitRoot

    if (tooManyImplicitRoots)
        throw IllegalArgumentException("Found more than one root ($implicitFuncName()/$implicitPropName) in $schema")

    return implicitRoot
}

fun ignoreContextFunc(member: KCallable<*>, ctx: KClass<*>): Boolean {
    if (member.valueParameters.isNotEmpty())
        return true

    if (member.visibility !== KVisibility.PUBLIC)
        return true

    if (member is KProperty2<*, *, *>)
        return true

    if (member.findAnnotation<GqlIgnore>() != null)
        return true

    val thisParam = member.instanceParameter ?: member.extensionReceiverParameter
    if (thisParam != null) {
        val klass = thisParam.type.classifier
        if (klass == null || klass !is KClass<*>)
            return true

        if (!klass.isSuperclassOf(ctx))
            return true

        if (klass == Any::class)
            return true
    }

    if (member is KFunction) {
        if (member.isInline || member.isInfix || member.isOperator || member.isSuspend)
            return true
    }

    val retType = member.returnType.classifier
    if (retType == null || retType == Any::class || retType !is KClass<*>)
        return true

    if (retType.qualifiedName?.startsWith("kotlin.") != false)
        return true

    if (member.returnType.arguments.isNotEmpty())
        return true

    return false
}

