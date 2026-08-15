package com.my.app.pytide.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

private val KEYWORDS = setOf(
    "False", "None", "True", "and", "as", "assert", "async", "await", "break",
    "class", "continue", "def", "del", "elif", "else", "except", "finally",
    "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
    "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
)

private val BUILTINS = setOf(
    "print", "len", "range", "input", "int", "str", "float", "list", "dict",
    "set", "tuple", "bool", "open", "type", "sum", "min", "max", "sorted",
    "enumerate", "zip", "map", "filter", "self"
)

private val keywordColor = Color(0xFF0033CC)
private val builtinColor = Color(0xFF9C27B0)
private val stringColor = Color(0xFF067D17)
private val commentColor = Color(0xFF9E9E9E)
private val numberColor = Color(0xFFB5651D)
private val defNameColor = Color(0xFF7A3E9D)

fun highlightPython(code: String): AnnotatedString = buildAnnotatedString {
    append(code)
    if (code.isEmpty()) return@buildAnnotatedString

    val stringRegex = Regex(
        "(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:\\\\.|[^\"\\\\\n])*\"|'(?:\\\\.|[^'\\\\\n])*')"
    )
    val commentRegex = Regex("#.*")
    val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    val defClassRegex = Regex("\\b(?:def|class)\\s+(\\w+)")
    val wordRegex = Regex("\\b[A-Za-z_]\\w*\\b")

    val occupied = BooleanArray(code.length)

    fun mark(range: IntRange) {
        for (i in range) if (i in code.indices) occupied[i] = true
    }

    for (m in stringRegex.findAll(code)) {
        addStyle(SpanStyle(color = stringColor), m.range.first, m.range.last + 1)
        mark(m.range)
    }
    for (m in commentRegex.findAll(code)) {
        addStyle(SpanStyle(color = commentColor), m.range.first, m.range.last + 1)
        mark(m.range)
    }
    for (m in numberRegex.findAll(code)) {
        if (occupied.getOrElse(m.range.first) { false }) continue
        addStyle(SpanStyle(color = numberColor), m.range.first, m.range.last + 1)
    }
    for (m in defClassRegex.findAll(code)) {
        val nameGroup = m.groups[1] ?: continue
        if (occupied.getOrElse(nameGroup.range.first) { false }) continue
        addStyle(
            SpanStyle(color = defNameColor, fontWeight = FontWeight.Bold),
            nameGroup.range.first, nameGroup.range.last + 1
        )
    }
    for (m in wordRegex.findAll(code)) {
        if (occupied.getOrElse(m.range.first) { false }) continue
        when (m.value) {
            in KEYWORDS -> addStyle(
                SpanStyle(color = keywordColor, fontWeight = FontWeight.Medium),
                m.range.first, m.range.last + 1
            )
            in BUILTINS -> addStyle(
                SpanStyle(color = builtinColor),
                m.range.first, m.range.last + 1
            )
        }
    }
}
