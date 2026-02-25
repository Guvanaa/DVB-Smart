package de.vvo.glassapp.ui.components

import android.os.Build
import androidx.compose.foundation.background
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
    return if (backdrop != null) {
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(if (isDark) 10f else 16f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        vibrancy()
                        lens(refractionHeight = 50f, refractionAmount = 200f)
                    }
                },
                onDrawSurface = {
                    // Lightmode: stärkere weiße Oberfläche für Kontrast und Lesbarkeit
                    drawRect(if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f))
                }
            )
            .padding(padding ?: 16.dp)
    } else {
        this
            .clip(shape)
            .background(if (isDark) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.80f))
            .padding(padding ?: 16.dp)
    }
}
