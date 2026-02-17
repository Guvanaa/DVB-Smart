package de.vvo.glassapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerAlignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.vvo.glassapp.ui.theme.GlassBlack
import de.vvo.glassapp.ui.theme.GlassWhite

@Composable
fun Modifier.glassEffect(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderWidth: Float = 1f
): Modifier {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) GlassBlack else GlassWhite
    val borderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f)

    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.7f),
                    backgroundColor.copy(alpha = 0.4f)
                )
            )
        )
        .border(
            width = borderWidth.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
        .padding(16.dp)
}
