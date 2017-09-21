package com.xs0.gqlktx.parser

class Token<out V: Any>(val row: Int, val column: Int, val type: Type, val rawValue: String, val value: V) {

    override fun toString(): String {
        return "$rawValue ($type @ $row:$column)"
    }

    fun `is`(rawText: String): Boolean {
        return rawText == this.rawValue
    }

    enum class Type {
        // literals:
        INTEGER,
        LONG, FLOAT, STRING,

        // name (identifier / keyword):
        NAME,

        // other:
        LPAREN,
        RPAREN, // ( )
        LCURLY, RCURLY, // { }
        LBRACK, RBRACK, // [ ]
        PIPE, // |
        COLON, // :
        SPREAD, // ...
        AT, // @
        EXCL, // !
        DOLLAR, // $
        EQ, // =

        EOF
    }
}
