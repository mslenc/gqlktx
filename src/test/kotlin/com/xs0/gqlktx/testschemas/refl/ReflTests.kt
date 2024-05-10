package com.xs0.gqlktx.testschemas.refl

import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.AutoBuilder
import org.junit.Test
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class ReflTests {
    private val schema: Schema<ReflTestSchema, Unit>
        get() = AutoBuilder.build(ReflTestSchema::class, Unit::class)

    @Test
    fun testSchemaBuild() {
        schema
    }

    @Test
    fun testIgnoredValsNotPresent() {
        var count = 0
        schema.introspector().type("ReflTest1").getFields(true)?.forEach { field ->
            count++
            when (field.name) {
                "entity" -> assert(true)
                else -> assert(false) { field.name + " - " + field.type }
            }
        }

        assert(count == 1)
    }
}


fun main() {
    println("Members:")
    for (member in ReflTest1::class.members) {
        println("" + member + " : " + member.annotations)
        if (member is KProperty1<*, *>) {
            println("... " + member.getter.annotations)
        }
    }
    println()
    println("Member properties:")
    for (member in ReflTest1::class.memberProperties)
        println("" + member + " : " + member.annotations)
    println()
    println("Member functions:")
    for (member in ReflTest1::class.memberFunctions)
        println("" + member + " : " + member.annotations)


}