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

import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build

@Composable
fun Modifier.glassEffect(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    borderWidth: Float = 1.2f,
    padding: Dp? = null
): Modifier {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) GlassBlack else GlassWhite
    val borderColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.5f)

    // True frosted glass effect for Android 12+
    // Using simple opacity fallback for sandbox build environment stability
    val blurModifier = Modifier

    return this
        .shadow(elevation = 16.dp, shape = shape, clip = false, spotColor = Color.Black.copy(alpha = 0.4f))
        .then(blurModifier)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = if (isDark) 0.55f else 0.45f),
                    backgroundColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                )
            )
        )
        .border(
            width = borderWidth.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
        .padding(padding ?: 16.dp)
}
