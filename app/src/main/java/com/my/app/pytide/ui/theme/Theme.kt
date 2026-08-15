package com.my.app.pytide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    background = BackgroundWhite,
    surface = SurfaceWhite,
    onBackground = TextBlack,
    onSurface = TextBlack
)

@Composable
fun PytIDETheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
