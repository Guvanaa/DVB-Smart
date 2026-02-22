package de.vvo.glassapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    padding: androidx.compose.ui.unit.Dp? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick).glassEffect(shape = shape, padding = padding)
        } else {
            modifier.glassEffect(shape = shape, padding = padding)
        }
    ) {
        content()
    }
}
