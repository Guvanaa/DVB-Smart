package de.vvo.glassapp.ui.screens

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
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
            MapLibre.getInstance(context, null)
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
                    manager.create(SymbolOptions()
                        .withLatLng(LatLng(pin.lat, pin.lon))
                        .withTextField(pin.line)
                        .withTextSize(13f)
                        .withTextColor(AndroidColor.BLACK)
                        .withTextHaloColor("white")
                        .withTextHaloWidth(1.5f)
                        .withIconImage("marker-15")
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
                        mapboxMap.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(51.0509, 13.7373))
                            .zoom(13.0)
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
