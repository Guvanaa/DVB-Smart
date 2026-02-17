package de.vvo.glassapp.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import de.vvo.glassapp.R
import de.vvo.glassapp.data.model.VehiclePin
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import de.vvo.glassapp.util.ServiceLocator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, stopId: String? = null) {
    val context = LocalContext.current
    val viewModel: TransitViewModel = remember { TransitViewModel(ServiceLocator.repository) }
    val mapView = remember { MapView(context) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    val pins by viewModel.mapPins.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val routeStops by viewModel.selectedVehicleRoute.collectAsState()

    LaunchedEffect(Unit) {
        Mapbox.getInstance(context, null)
        while(true) {
            // Simplified bounding box for Dresden
            viewModel.loadMapPins(51.0, 13.6, 51.1, 13.9)
            delay(15000)
        }
    }

    // Update symbols when pins change
    LaunchedEffect(pins, symbolManager) {
        symbolManager?.let { manager ->
            manager.deleteAll()
            pins.forEach { pin ->
                val punctualityColor = when {
                    (pin.punctuality ?: 0) < 0 -> "#4CAF50" // Early (Green)
                    (pin.punctuality ?: 0) == 0 -> "#FFCC00" // Punctual (DVB Yellow)
                    else -> "#F44336" // Delayed (Red)
                }

                manager.create(SymbolOptions()
                    .withLatLng(LatLng(pin.lat, pin.lon))
                    .withTextField(pin.line)
                    .withTextSize(12f)
                    .withTextColor(AndroidColor.BLACK)
                    .withTextHaloColor("white")
                    .withTextHaloWidth(1f)
                    .withIconImage("circle-15") // Standard Mapbox icon
                    .withIconColor(punctualityColor)
                    .withData(com.google.gson.JsonPrimitive(pin.id))
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.getMapAsync { mapboxMap ->
                    if (mapboxMap.style == null) {
                        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                            val manager = SymbolManager(mv, mapboxMap, style)
                            manager.iconAllowOverlap = true
                            manager.textAllowOverlap = true
                            manager.addClickListener { symbol ->
                                val vehicleId = symbol.data?.asString
                                val vehicle = pins.find { it.id == vehicleId }
                                if (vehicle != null) {
                                    viewModel.selectVehicle(vehicle)
                                }
                                true
                            }
                            symbolManager = manager
                        }
                        mapboxMap.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(51.0509, 13.7373))
                            .zoom(13.0)
                            .build()
                    }
                }
            }
        )

        // Overlay UI
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
        ) {
            GlassCard(modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
            }
        }

        // Selected Vehicle Info & Thermometer
        if (selectedVehicle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                RouteDetailSheet(
                    stops = routeStops,
                    onStopClick = { /* Show transfers? */ }
                )
            }

            // Close button for selected vehicle
            IconButton(
                onClick = { viewModel.deselectVehicle() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                GlassCard(modifier = Modifier.size(32.dp)) {
                    Text("X", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // General info
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.live_traffic), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.vehicles_nearby, pins.size), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
