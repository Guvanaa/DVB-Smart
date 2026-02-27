package de.vvo.glassapp.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens

val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun Modifier.glassEffect(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    borderWidth: Float = 1.2f,
    padding: Dp? = null
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.13f)
    return if (backdrop != null) {
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(if (isDark) 10f else 20f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        vibrancy()
                        lens(refractionHeight = 50f, refractionAmount = 200f)
                    }
                },
                onDrawSurface = {
                    drawRect(if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f))
                }
            )
            .border(width = borderWidth.dp, color = borderColor, shape = shape)
            .padding(padding ?: 16.dp)
    } else {
        this
            .border(width = borderWidth.dp, color = borderColor, shape = shape)
            .clip(shape)
            .background(if (isDark) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.92f))
            .padding(padding ?: 16.dp)
    }
}
