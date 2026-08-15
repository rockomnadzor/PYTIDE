package com.my.app.pytide.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TransformedText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun applyAutoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val insertedNewline = new.text.length == old.text.length + 1 &&
        new.selection.collapsed &&
        new.selection.start > 0 &&
        new.text.getOrNull(new.selection.start - 1) == '\n'

    if (!insertedNewline) return new

    val cursor = new.selection.start
    val lineStart = new.text.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
    val prevLine = new.text.substring(lineStart, cursor - 1)
    val leadingSpaces = prevLine.takeWhile { it == ' ' }
    val extra = if (prevLine.trimEnd().endsWith(":")) "    " else ""
    val indent = leadingSpaces + extra

    if (indent.isEmpty()) return new

    val newText = new.text.substring(0, cursor) + indent + new.text.substring(cursor)
    return TextFieldValue(newText, TextRange(cursor + indent.length))
}

@Composable
fun CodeEditorField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineCount = value.text.count { it == '\n' } + 1
    val lineNumbers = (1..lineCount).joinToString("\n") { it.toString() }

    Row(modifier = modifier.fillMaxSize()) {
        Text(
            text = lineNumbers,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFFB0B0B0),
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(end = 8.dp)
                .widthIn(min = 28.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(applyAutoIndent(value, it)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF1A1A1A)
            ),
            visualTransformation = { text ->
                TransformedText(highlightPython(text.text), OffsetMapping.Identity)
            },
            cursorBrush = SolidColor(Color(0xFF2962FF))
        )
    }
}
