package com.xs0.gqlktx.testschemas.spec

import com.xs0.gqlktx.schema.Schema
import com.xs0.gqlktx.schema.builder.AutoBuilder
import io.vertx.core.json.JsonObject
import org.junit.Test


class TheTest {
    @Test
    fun testSchemaBuild() {
        schema
    }

    private val schema: Schema<SpecTestSchema, Unit>
        get() = AutoBuilder.build(SpecTestSchema::class, Unit::class)

    private fun assertValidationSucceeds(query: String, opName: String?, parameters: JsonObject?) {

    }

    private fun assertValidationFails(query: String, opName: String?, parameters: JsonObject?) {

    }

    @Test
    fun testOperationNameUniquenessValid() {
        // see https://facebook.github.io/graphql/#sec-Operation-Name-Uniqueness
        val query = "query getDogName {\n" +
                "  dog {\n" +
                "    name\n" +
                "  }\n" +
                "}\n" +
                "query getOwnerName {\n" +
                "  dog {\n" +
                "    owner {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}"

        assertValidationSucceeds(query, "getDogName", null)
        assertValidationSucceeds(query, "getOwnerName", null)
    }

    @Test
    fun testOperationNameUniquenessInvalid() {
        // see https://facebook.github.io/graphql/#sec-Operation-Name-Uniqueness
        val query = "query getName {\n" +
                "  dog {\n" +
                "    name\n" +
                "  }\n" +
                "}\n" +
                "query getName {\n" +
                "  dog {\n" +
                "    owner {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}"

        assertValidationFails(query, "getName", null)
        assertValidationFails(query, null, null)
    }

    @Test
    fun testOperationNameUniquenessInvalid2() {
        // see https://facebook.github.io/graphql/#sec-Operation-Name-Uniqueness
        val query = "query dogOperation {\n" +
                "  dog {\n" +
                "    name\n" +
                "  }\n" +
                "}\n" +
                "mutation dogOperation {\n" +
                "  mutateDog {\n" +
                "    id\n" +
                "  }\n" +
                "}"

        assertValidationFails(query, "dogOperation", null)
        assertValidationFails(query, null, null)
    }

    @Test
    fun testLoneAnonymousOperationValid() {
        // see https://facebook.github.io/graphql/#sec-Lone-Anonymous-Operation
        val query = "{\n" +
                "  dog {\n" +
                "    name\n" +
                "  }\n" +
                "}"

        assertValidationSucceeds(query, null, null)
        assertValidationFails(query, "getName", null)
    }

    @Test
    fun testLoneAnonymousOperationInvalid() {
        // see https://facebook.github.io/graphql/#sec-Lone-Anonymous-Operation
        val query = "{\n" +
                "  dog {\n" +
                "    name\n" +
                "  }\n" +
                "}\n" +
                "query getName {\n" +
                "  dog {\n" +
                "    owner {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}"

        assertValidationFails(query, null, null)
        assertValidationFails(query, "getName", null)
        assertValidationFails(query, "whatever", null)
    }

    // TODO: https://facebook.github.io/graphql/#sec-Single-root-field

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsValid() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types

        val query = "{ dog { ...interfaceFieldSelection } }\n" +
                "fragment interfaceFieldSelection on Pet {\n" +
                "  name\n" +
                "}"

        assertValidationSucceeds(query, null, null)
    }

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsValid2() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types

        val query = "{ dog { ...inDirectFieldSelectionOnUnion } }\n" +
                "fragment inDirectFieldSelectionOnUnion on CatOrDog {\n" +
                "  __typename\n" +
                "  ... on Pet {\n" +
                "    name\n" +
                "  }\n" +
                "  ... on Dog {\n" +
                "    barkVolume\n" +
                "  }\n" +
                "}"

        assertValidationSucceeds(query, null, null)
    }

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsInvalid() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types
        val query = "{ dog { ...fieldNotDefined } }\n" +
                "fragment fieldNotDefined on Dog {\n" +
                "  meowVolume\n" +
                "}"

        assertValidationFails(query, null, null)
    }

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsInvalid2() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types
        val query = "{ dog { ...aliasedLyingFieldTargetNotDefined } }\n" +
                "fragment aliasedLyingFieldTargetNotDefined on Dog {\n" +
                "  barkVolume: kawVolume\n" +
                "}"

        assertValidationFails(query, null, null)
    }

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsInvalid3() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types
        val query = "{ dog { ...definedOnImplementorsButNotInterface } }\n" +
                "fragment definedOnImplementorsButNotInterface on Pet {\n" +
                "  nickname\n" +
                "}"

        assertValidationFails(query, null, null)
    }

    @Test
    fun testFieldSelectionsOnObjectsInterfacesUnionsInvalid4() {
        // see https://facebook.github.io/graphql/#sec-Field-Selections-on-Objects-Interfaces-and-Unions-Types
        val query = "{ dog { ...directFieldSelectionOnUnion } }\n" +
                "fragment directFieldSelectionOnUnion on CatOrDog {\n" +
                "  name\n" +
                "  barkVolume\n" +
                "}"

        assertValidationFails(query, null, null)
    }

    @Test
    fun testFieldSelectionMergingValid() {
        // see https://facebook.github.io/graphql/#sec-Field-Selection-Merging
        val query = "{ dog { ... mergeIdenticalFields } }\n" +
                "fragment mergeIdenticalFields on Dog {\n" +
                "  name\n" +
                "  name\n" +
                "}"

        assertValidationSucceeds(query, null, null)
    }

    @Test
    fun testFieldSelectionMergingValid2() {
        // see https://facebook.github.io/graphql/#sec-Field-Selection-Merging
        val query = "{ dog { ... mergeIdenticalAliasesAndFields } }\n" +
                "fragment mergeIdenticalAliasesAndFields on Dog {\n" +
                "  otherName: name\n" +
                "  otherName: name\n" +
                "}"

        assertValidationSucceeds(query, null, null)
    }

    @Test
    fun testFieldSelectionMergingInvalid() {
        // see https://facebook.github.io/graphql/#sec-Field-Selection-Merging
        val query = "{ dog { ... conflictingBecauseAlias } }\n" +
                "fragment conflictingBecauseAlias on Dog {\n" +
                "  name: nickname\n" +
                "  name\n" +
                "}"

        assertValidationFails(query, null, null)
    }
}
