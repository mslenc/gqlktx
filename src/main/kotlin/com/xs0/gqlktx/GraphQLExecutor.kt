package com.xs0.gqlktx

import com.github.mslenc.utils.getLogger
import com.xs0.gqlktx.codegen.executorClassName
import com.xs0.gqlktx.dom.OpType
import com.xs0.gqlktx.dom.OperationDefinition
import com.xs0.gqlktx.exec.SimpleQueryExecutor
import com.xs0.gqlktx.parser.GraphQLParser
import com.xs0.gqlktx.schema.builder.AutoBuilder
import com.xs0.gqlktx.utils.QueryInput
import kotlin.reflect.KClass

enum class GqlEnginePref {
    REFLECTION_ONLY,
    CODE_GEN_ONLY,
    REFLECTION_PREFERRED,
    CODE_GEN_PREFERRED,
    COMPARE
}

interface GraphQLExecutor<ROOT: Any, CTX: Any> {
    suspend fun execute(rootObject: ROOT, context: CTX, queryInput: QueryInput, scalarCoercion: ScalarCoercion = ScalarCoercion.JSON): Map<String, Any?>

    companion object {
        val logger = getLogger<GraphQLExecutor<*, *>>()

        fun <ROOT: Any, CTX: Any> create(rootClass: KClass<ROOT>, ctxClass: KClass<CTX>, enginePref: GqlEnginePref = GqlEnginePref.CODE_GEN_PREFERRED): GraphQLExecutor<ROOT, CTX> {
            return when (enginePref) {
                GqlEnginePref.REFLECTION_ONLY -> createReflection(rootClass, ctxClass)
                GqlEnginePref.CODE_GEN_ONLY -> createCodeGen(rootClass, ctxClass)
                GqlEnginePref.REFLECTION_PREFERRED -> createReflection(rootClass, ctxClass) ?: createCodeGen(rootClass, ctxClass)
                GqlEnginePref.CODE_GEN_PREFERRED -> createCodeGen(rootClass, ctxClass) ?: createReflection(rootClass, ctxClass)
                GqlEnginePref.COMPARE -> createCompare(create(rootClass, ctxClass, GqlEnginePref.REFLECTION_ONLY), create(rootClass, ctxClass, GqlEnginePref.CODE_GEN_ONLY))
            } ?: throw IllegalStateException("Couldn't create an executor.")
        }

        private fun <ROOT: Any, CTX: Any> createReflection(rootClass: KClass<ROOT>, ctxClass: KClass<CTX>): GraphQLExecutor<ROOT, CTX>? {
            val schema = try {
                AutoBuilder.build(rootClass, ctxClass).also {
                    logger.info("Built GraphQL executor with reflection.")
                }
            } catch (e: Exception) {
                logger.warn("Couldn't create GraphQL executor with reflection.", e)
                return null
            }

            return object : GraphQLExecutor<ROOT, CTX> {
                override suspend fun execute(rootObject: ROOT, context: CTX, queryInput: QueryInput, scalarCoercion: ScalarCoercion): Map<String, Any?> {
                    return SimpleQueryExecutor.execute(schema, rootObject, context, queryInput, scalarCoercion)
                }
            }
        }

        private fun <ROOT: Any, CTX: Any> createCodeGen(rootClass: KClass<ROOT>, ctxClass: KClass<CTX>): GraphQLExecutor<ROOT, CTX>? {
            val className = executorClassName(rootClass).let { it.first + "." + it.second }

            return try {
                (Class.forName(className).kotlin.objectInstance as GraphQLExecutor<ROOT, CTX>).also {
                    logger.info("Using GraphQL executor from generated code.")
                }
            } catch (e: Exception) {
                logger.warn("Couldn't find GraphQL executor from generated code.")
                return null
            }
        }

        private fun <ROOT: Any, CTX: Any> createCompare(a: GraphQLExecutor<ROOT, CTX>, b: GraphQLExecutor<ROOT, CTX>): GraphQLExecutor<ROOT, CTX> {
            return object : GraphQLExecutor<ROOT, CTX> {
                override suspend fun execute(rootObject: ROOT, context: CTX, queryInput: QueryInput, scalarCoercion: ScalarCoercion): Map<String, Any?> {
                    val isMutation = when {
                        !queryInput.allowMutations -> false
                        else -> {
                            val query = GraphQLParser.parseQueryDoc(queryInput.query)
                            val op = when {
                                queryInput.opName != null -> query.definitions.first { (it as? OperationDefinition)?.name == queryInput.opName }
                                else -> query.definitions.mapNotNull { it as? OperationDefinition }.single()
                            } as OperationDefinition
                            op.type == OpType.MUTATION
                        }
                    }

                    if (isMutation)
                        return a.execute(rootObject, context, queryInput)

                    val resultA = a.execute(rootObject, context, queryInput, scalarCoercion)
                    val resultB = b.execute(rootObject, context, queryInput, scalarCoercion)

                    if (resultA == resultB) {
                        println("They are equal!")
                    } else {
                        println("They are NOT equal!")
                    }

                    return resultA
                }
            }
        }
    }
}

