package de.vvo.glassapp.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable

import de.vvo.glassapp.ui.components.glassEffect

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    padding: androidx.compose.ui.unit.Dp? = null,
    borderWidth: Float = 1.2f,
    surfaceColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shadowedModifier = if (!isDark) {
        modifier.shadow(elevation = 6.dp, shape = shape, clip = false)
    } else {
        modifier
    }

    Box(
        modifier = if (onClick != null) {
            shadowedModifier.clickable(onClick = onClick).glassEffect(shape = shape, padding = padding, borderWidth = borderWidth, surfaceColor = surfaceColor, borderColor = borderColor)
        } else {
            shadowedModifier.glassEffect(shape = shape, padding = padding, borderWidth = borderWidth, surfaceColor = surfaceColor, borderColor = borderColor)
        }
    ) {
        content()
    }
}
