package com.xs0.gqlktx

/**
 * The type of output coercion to perform on scalar values.
 *
 * The overall result is always a structure consisting of Maps, Lists and scalar values. Because the usual output format
 * is JSON, the scalar values can be coerced to JSON-compatible types. However, if the result of a query is to be
 * processed further rather than encoded into JSON, the coercion is likely counter-productive, as it loses almost all
 * type information.
 */
enum class ScalarCoercion {
    /**
     * Only the scalar types directly representable in JS are produced - String, Boolean and Double.
     */
    STRICT_JSON,

    /**
     * Like STRICT_JSON, but additional number types are allowed for improved precision - String, Boolean, Double,
     * Integer, Long and BigDecimal. Meant for encoding in JSON, but with a non-JS language on both sides,
     * the numbers should stay exact.
     */
    JSON,

    /**
     * Produces values that might be appropriate for later emitting to a spreadsheet - numbers, strings and booleans
     * like with JSON, but dates and times remain themselves, instead of being converted to strings. To reduce the
     * number of types that need to be handled, bytes and shorts become integers and floats become doubles. The other
     * types are not likely to be useful, so chars, char arrays, byte arrays, enums, node IDs and UUIDs are still
     * converted to strings.
     */
    SPREADSHEET,

    /**
     * Leaves all scalars as-is. Meant for further consumption within the same JVM.
     */
    NONE
}