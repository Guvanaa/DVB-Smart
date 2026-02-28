package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.vvo.glassapp.data.model.Stop
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.components.LocalGlassBackdrop
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Position

// Minimalistischer Style: graue Straßen + blaue OSM-Haltestellenpunkte + rote VVO-Stops
// Nutzt OpenFreeMap-Vektorkacheln (OpenMapTiles Schema)
// WICHTIG: stops-source und proto-stop-dots Layer sind im JSON definiert,
// damit sie beim Style-Load existieren (bevor Compose-Layer hinzugefügt werden).
// Hintergrund: maplibre-compose fügt Compose-Layer via onEndChanges() hinzu,
// BEVOR DisposableEffect (SourceReferenceEffect) die Source hinzufügt → Layer schlägt fehl.
// Lösung: Source + Layer direkt im Base-Style JSON definieren.
private fun buildStopStyle(initialGeoJson: String) = BaseStyle.Json("""
{
  "version": 8,
  "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
  "sources": {
    "omtiles": {
      "type": "vector",
      "url": "https://tiles.openfreemap.org/planet"
    },
    "stops-source": {
      "type": "geojson",
      "data": $initialGeoJson
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
        "circle-radius": 6,
        "circle-color": "#1976D2",
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 2
      }
    },
    {
      "id": "proto-stop-dots",
      "type": "circle",
      "source": "stops-source",
      "paint": {
        "circle-radius": 10,
        "circle-color": "#FF0000",
        "circle-stroke-color": "#ffffff",
        "circle-stroke-width": 2
      }
    },
    {
      "id": "proto-stop-labels",
      "type": "symbol",
      "source": "stops-source",
      "layout": {
        "text-field": ["get", "name"],
        "text-font": ["Noto Sans Regular"],
        "text-size": 11,
        "text-offset": [0, 1.2],
        "text-anchor": "top",
        "text-allow-overlap": false,
        "text-ignore-placement": false
      },
      "paint": {
        "text-color": "#CC0000",
        "text-halo-color": "#ffffff",
        "text-halo-width": 1.5
      }
    }
  ]
}
""")

@Composable
fun GlassProtoScreen(navController: NavController) {
    val viewModel: TransitViewModel = viewModel(factory = TransitViewModel.Factory)
    val mapStops by viewModel.mapStops.collectAsState()
    var selectedStop by remember { mutableStateOf<Stop?>(null) }

    LaunchedEffect(Unit) {
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

    // Use StyleState to update the base style source with VVO stop data
    val styleState = rememberStyleState()

    // Compute GeoJSON when stops are loaded and push to native source
    LaunchedEffect(mapStops, styleState.sources) {
        val source = styleState.sources["stops-source"] as? GeoJsonSource ?: return@LaunchedEffect
        if (mapStops.isEmpty()) return@LaunchedEffect
        val featuresJson = mapStops.mapNotNull { stop ->
            val sLat = stop.latitudeValue() ?: return@mapNotNull null
            val sLon = stop.longitudeValue() ?: return@mapNotNull null
            if (sLat == 0.0 && sLon == 0.0) return@mapNotNull null
            val idJson = JsonPrimitive(stop.id)
            val nameJson = JsonPrimitive(stop.name)
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[$sLon,$sLat]},"properties":{"id":$idJson,"name":$nameJson}}"""
        }.joinToString(",")
        source.setData(GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$featuresJson]}"""))
    }

    // Initial style has one test dot at Dresden Neustadt to verify rendering.
    // Once mapStops loads, LaunchedEffect above updates the source with real data.
    val baseStyle = remember {
        buildStopStyle("""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[13.7373,51.0509]},"properties":{"id":"init","name":"INIT"}}]}""")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            baseStyle = baseStyle,
            styleState = styleState,
            cameraState = cameraState,
            options = MapOptions(
                renderOptions = RenderOptions(
                    renderMode = RenderOptions.RenderMode.TextureView
                )
            )
        )

        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {

            // Back-Button oben links
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

            // Stop-Info-Panel unten
            selectedStop?.let { stop ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stop.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                stop.place?.let {
                                    Text(
                                        text = it,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            TextButton(onClick = { navController.navigate("map/${stop.id}") }) {
                                Text("Karte →", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    IconButton(
                        onClick = { selectedStop = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
