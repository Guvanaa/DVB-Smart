package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.components.LocalGlassBackdrop
import de.vvo.glassapp.ui.components.StopDetailSheet
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

// Basemap style: minimalist roads + blue OSM transit stops (no VVO source here – managed via compose layers below)
private val BASE_STYLE = BaseStyle.Json("""
{
  "version": 8,
  "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
  "sources": {
    "omtiles": {
      "type": "vector",
      "url": "https://tiles.openfreemap.org/planet"
    }
  },
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {"background-color": "#f2f2f0"}
    },
    {
      "id": "water",
      "type": "fill",
      "source": "omtiles",
      "source-layer": "water",
      "paint": {"fill-color": "#aad3df"}
    },
    {
      "id": "road-service",
      "type": "line",
      "source": "omtiles",
      "source-layer": "transportation",
      "filter": ["in", "class", "service", "track", "path"],
      "paint": {"line-color": "#e0e0e0", "line-width": 1}
    },
    {
      "id": "road-minor",
      "type": "line",
      "source": "omtiles",
      "source-layer": "transportation",
      "filter": ["in", "class", "minor"],
      "paint": {"line-color": "#d0d0d0", "line-width": 1.5}
    },
    {
      "id": "road-secondary",
      "type": "line",
      "source": "omtiles",
      "source-layer": "transportation",
      "filter": ["in", "class", "secondary", "tertiary"],
      "paint": {"line-color": "#b8b8b8", "line-width": 2.5}
    },
    {
      "id": "road-primary",
      "type": "line",
      "source": "omtiles",
      "source-layer": "transportation",
      "filter": ["in", "class", "primary"],
      "paint": {"line-color": "#a0a0a0", "line-width": 4}
    },
    {
      "id": "road-trunk-motorway",
      "type": "line",
      "source": "omtiles",
      "source-layer": "transportation",
      "filter": ["in", "class", "trunk", "motorway"],
      "paint": {"line-color": "#888888", "line-width": 5}
    },
    {
      "id": "transit-stops",
      "type": "circle",
      "source": "omtiles",
      "source-layer": "poi",
      "filter": ["in", "class", "bus", "railway"],
      "paint": {
        "circle-radius": 4,
        "circle-color": "#1976D2",
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 1.5
      }
    }
  ]
}
""")

@Composable
fun GlassProtoScreen(navController: NavController) {
    val viewModel: TransitViewModel = viewModel(factory = TransitViewModel.Factory)
    val osmStops by viewModel.overpassStops.collectAsState()
    val vvoStops by viewModel.mapStops.collectAsState()
    val departures by viewModel.departures.collectAsState()
    val selectedStop by viewModel.selectedStop.collectAsState()

    // Primary: OSM via Overpass (accurate WGS84). Fallback: VVO MapPins with improved GK4 math.
    val displayStops = if (osmStops.isNotEmpty()) osmStops else vvoStops

    LaunchedEffect(Unit) {
        viewModel.loadOverpassStops(51.020, 13.660, 51.090, 13.830)
        // Also trigger VVO MapPins as fallback (used when Overpass is unavailable)
        viewModel.loadMapData(51.030, 13.690, 51.080, 13.800)
    }

    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) { onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) } }
    val backdrop = rememberLayerBackdrop(graphicsLayer)

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(13.7373, 51.0509),
            zoom = 14.0
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        MaplibreMap(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            baseStyle = BASE_STYLE,
            cameraState = cameraState,
            options = MapOptions(
                renderOptions = RenderOptions(renderMode = RenderOptions.RenderMode.TextureView)
            )
        ) {
            // ── Stop dots from Overpass / OSM (WGS84, no coordinate conversion) ──
            val stopSource = rememberGeoJsonSource(remember(displayStops) {
                val featuresJson = displayStops.mapNotNull { stop ->
                    val sLat = stop.latitudeValue() ?: return@mapNotNull null
                    val sLon = stop.longitudeValue() ?: return@mapNotNull null
                    if (sLat == 0.0 && sLon == 0.0) return@mapNotNull null
                    val idJson   = JsonPrimitive(stop.id)
                    val nameJson = JsonPrimitive(stop.name)
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[$sLon,$sLat]},"properties":{"id":$idJson,"name":$nameJson}}"""
                }.joinToString(",")
                GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$featuresJson]}""")
            })

            CircleLayer(
                id = "proto-stop-dots",
                source = stopSource,
                radius = const(9.dp),
                color = const(Color(0xFFFF3B30)),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { features ->
                    val id   = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content
                    val stop = displayStops.find { it.id == id }
                    if (stop != null) {
                        // selectOsmStop resolves the VVO numeric ID via name search
                        viewModel.selectOsmStop(stop)
                        ClickResult.Consume
                    } else {
                        ClickResult.Pass
                    }
                }
            )
        }

        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {

            // Back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(20.dp)
                    .statusBarsPadding()
                    .align(Alignment.TopStart)
            ) {
                GlassCard(modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            }

            // Stop detail sheet (departures) when a stop is selected
            selectedStop?.let { stop ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    StopDetailSheet(stop = stop, departures = departures)
                    IconButton(
                        onClick = { viewModel.deselectAll() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
