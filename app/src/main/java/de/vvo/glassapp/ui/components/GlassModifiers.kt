package de.vvo.glassapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.vvo.glassapp.ui.theme.GlassBlack
import de.vvo.glassapp.ui.theme.GlassWhite

import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.glassEffect(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    borderWidth: Float = 1.2f,
    padding: Dp? = null
): Modifier {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) GlassBlack else GlassWhite
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)

    return this
        .shadow(elevation = 12.dp, shape = shape, clip = false, spotColor = Color.Black.copy(alpha = 0.3f))
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = if (isDark) 0.6f else 0.5f),
                    backgroundColor.copy(alpha = if (isDark) 0.3f else 0.2f)
                )
            )
        )
        .border(
            width = borderWidth.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
        .padding(padding ?: 16.dp)
}
