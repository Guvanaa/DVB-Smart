package de.vvo.glassapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.components.LocalGlassBackdrop
import de.vvo.glassapp.ui.theme.DvbYellow

@Composable
fun GlassProtoScreen(navController: NavController) {
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) { onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) } }
    val backdrop = rememberLayerBackdrop(graphicsLayer)

    val inf = rememberInfiniteTransition()
    val t1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val t2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse))
    val t3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    // Kreis der von links nach rechts durch die Glaskarten läuft
    val t4 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart))

    Box(modifier = Modifier.fillMaxSize()) {

        // Backdrop-Quelle: reiner Compose-Inhalt mit bewegten Elementen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Color(0xFF0D1B2A))
        ) {
            // Grünfläche (Park)
            Box(
                modifier = Modifier
                    .offset(x = (t1 * 220).dp, y = (t2 * 80 + 60).dp)
                    .size(200.dp)
                    .background(Color(0xFF2E7D32), CircleShape)
            )
            // Wasserfläche (Fluss)
            Box(
                modifier = Modifier
                    .offset(x = (t2 * -60 + 20).dp, y = (t1 * 150 + 350).dp)
                    .size(280.dp, 70.dp)
                    .background(Color(0xFF1565C0), RoundedCornerShape(35.dp))
            )
            // Breite Straße
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = (t3 * 200 + 250).dp)
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(Color(0xFFEEEEEE))
            )
            // Schmale Querstraße
            Box(
                modifier = Modifier
                    .offset(x = (t1 * 180 + 80).dp, y = 0.dp)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFBDBDBD))
            )
            // Gebäudeblock
            Box(
                modifier = Modifier
                    .offset(x = (t3 * 120 + 150).dp, y = (t2 * 120 + 120).dp)
                    .size(90.dp, 70.dp)
                    .background(Color(0xFFBF360C), RoundedCornerShape(6.dp))
            )
            // Kleines Gebäude
            Box(
                modifier = Modifier
                    .offset(x = (t2 * 80 + 40).dp, y = (t1 * 80 + 480).dp)
                    .size(55.dp)
                    .background(Color(0xFFE65100), RoundedCornerShape(6.dp))
            )
            // Gelbe Straßenbahn-Linie
            Box(
                modifier = Modifier
                    .offset(x = (t1 * -100).dp, y = (t3 * 100 + 550).dp)
                    .size(350.dp, 8.dp)
                    .background(DvbYellow)
            )
            // Heller Kreis der durch die Glaskarten läuft (von links nach rechts, endlos)
            Box(
                modifier = Modifier
                    .offset(x = (t4 * 600 - 150).dp, y = 620.dp)
                    .size(130.dp)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.9f), CircleShape)
            )
        }

        // Glass-UI über dem Compose-Inhalt
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    GlassCard(modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text("Liquid Glass Prototyp", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Blur + Brechung über bewegtem Compose-Inhalt", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Linie 12  →  Striesen", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("+2 min", color = Color(0xFFF44336), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
