package com.meowcha.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Pink {
    val Bg = Color(0xFFFCE4EC)
    val Light = Color(0xFFF8BBD0)
    val Main = Color(0xFFF06292)
    val Deep = Color(0xFFD81B60)
    val Cream = Color(0xFFFFF8F0)
    val Lilac = Color(0xFFE1BEE7)
    val Text = Color(0xFF6A1B4D)
    val Wood = Color(0xFFE8B4A0)
    val Gold = Color(0xFFFFC107)
}

@Composable
fun MeowchaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pink.Main,
            onPrimary = Color.White,
            secondary = Pink.Lilac,
            background = Pink.Bg,
            surface = Pink.Cream,
            onSurface = Pink.Text,
            onBackground = Pink.Text,
        ),
        content = content,
    )
}
