package de.vvo.glassapp.ui.screens

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
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
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
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
fun MapScreen(
    navController: NavController,
    stopId: String? = null,
    lat: Double? = null,
    lon: Double? = null
) {
    val context = LocalContext.current
    val viewModel: TransitViewModel = remember { TransitViewModel(ServiceLocator.repository) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var lineManager by remember { mutableStateOf<LineManager?>(null) }

    val pins by viewModel.mapPins.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val routeStops by viewModel.selectedVehicleRoute.collectAsState()

    LaunchedEffect(Unit) {
        try {
            Mapbox.getInstance(context)
        } catch (e: Exception) { }

        while(true) {
            viewModel.loadMapPins(51.0, 13.6, 51.1, 13.9)
            delay(10000)
        }
    }

    // Update symbols
    LaunchedEffect(pins, symbolManager) {
        symbolManager?.let { manager ->
            try {
                manager.deleteAll()
                pins.forEach { pin ->
                    val color = when {
                        (pin.punctuality ?: 0) < 0 -> "#4CAF50"
                        (pin.punctuality ?: 0) == 0 -> "#FFCC00"
                        else -> "#F44336"
                    }
                    val icon = when (pin.type) {
                        "Tram" -> "tram"
                        "CityBus", "Bus" -> "bus"
                        "SuburbanRailway", "Train" -> "rail"
                        else -> "marker-15"
                    }
                    manager.create(SymbolOptions()
                        .withLatLng(LatLng(pin.lat, pin.lon))
                        .withTextField(pin.line)
                        .withTextSize(11f)
                        .withTextColor("#000000")
                        .withTextHaloColor("#FFFFFF")
                        .withTextHaloWidth(2.0f)
                        .withIconImage(icon)
                        .withIconColor(color)
                        .withData(com.google.gson.JsonPrimitive(pin.id))
                    )
                }
            } catch (e: Exception) { }
        }
    }

    // Update route line
    LaunchedEffect(routeStops, lineManager) {
        lineManager?.let { manager ->
            manager.deleteAll()
            if (routeStops.isNotEmpty()) {
                val points = routeStops.map { LatLng(it.lat, it.lon) }
                manager.create(LineOptions()
                    .withLatLngs(points)
                    .withLineColor("#FFCC00")
                    .withLineWidth(4f)
                    .withLineOpacity(0.8f)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                MapView(context).apply {
                    onCreate(null)
                    getMapAsync { mapboxMap ->
                        val styleUrl = "https://demotiles.maplibre.org/style.json"
                        mapboxMap.setStyle(styleUrl) { style ->
                            symbolManager = SymbolManager(this, mapboxMap, style).apply {
                                iconAllowOverlap = true
                                textAllowOverlap = true
                                addClickListener { symbol ->
                                    val vehicleId = symbol.data?.asString
                                    pins.find { it.id == vehicleId }?.let { viewModel.selectVehicle(it) }
                                    true
                                }
                            }
                            lineManager = LineManager(this, mapboxMap, style)
                        }
                        val target = if (lat != null && lon != null) {
                            LatLng(lat, lon)
                        } else {
                            LatLng(51.0509, 13.7373)
                        }

                        mapboxMap.cameraPosition = CameraPosition.Builder()
                            .target(target)
                            .zoom(if (lat != null) 15.0 else 13.0)
                            .build()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(20.dp).statusBarsPadding().align(Alignment.TopStart)
        ) {
            GlassCard(modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
        }

        IconButton(
            onClick = {
                // In a real app, we would animate to user location
            },
            modifier = Modifier.padding(20.dp).statusBarsPadding().align(Alignment.TopEnd)
        ) {
            GlassCard(modifier = Modifier.size(52.dp)) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Locate Me",
                    tint = Color.White
                )
            }
        }

        if (selectedVehicle != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding()
            ) {
                RouteDetailSheet(stops = routeStops, onStopClick = { })
                IconButton(
                    onClick = { viewModel.deselectVehicle() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).navigationBarsPadding()
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(stringResource(R.string.live_traffic), fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                        Text(stringResource(R.string.vehicles_nearby, pins.size), color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
