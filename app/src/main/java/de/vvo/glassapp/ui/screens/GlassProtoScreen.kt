package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.layout.*
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
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
fun GlassProtoScreen(navController: NavController) {
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) { onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) } }
    val backdrop = rememberLayerBackdrop(graphicsLayer)

    // Position(longitude, latitude) – GeoJSON Konvention
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(13.7373, 51.0509),
            zoom = 14.0
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Backdrop-Quelle: echte MapLibre-Karte (Test ob GraphicsLayer sie erfassen kann)
        MaplibreMap(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/bright"),
            cameraState = cameraState,
            options = MapOptions(
                renderOptions = RenderOptions(
                    renderMode = RenderOptions.RenderMode.TextureView
                )
            )
        )

        // Glass-UI über der Karte
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
                        Text("Liquid Glass Test", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Blur + Brechung über echter MapLibre-Karte", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
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
