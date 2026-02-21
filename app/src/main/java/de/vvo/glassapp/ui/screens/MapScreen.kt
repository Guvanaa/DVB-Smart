package de.vvo.glassapp.ui.screens

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val viewModel: TransitViewModel = viewModel(factory = TransitViewModel.Factory)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        // Simulate user location for this demo
        viewModel.updateUserLocation(51.0509, 13.7373)
    }

    var mapInstance by remember { mutableStateOf<com.mapbox.mapboxsdk.maps.MapboxMap?>(null) }
    var vehicleManager by remember { mutableStateOf<SymbolManager?>(null) }
    var stopManager by remember { mutableStateOf<SymbolManager?>(null) }
    var poiManager by remember { mutableStateOf<SymbolManager?>(null) }
    var lineManager by remember { mutableStateOf<LineManager?>(null) }

    val pins by viewModel.mapPins.collectAsState()
    val stops by viewModel.mapStops.collectAsState()
    val departures by viewModel.departures.collectAsState()
    val userLocationState by viewModel.userLocation.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val selectedStop by viewModel.selectedStop.collectAsState()
    val routeStops by viewModel.selectedVehicleRoute.collectAsState()

    LaunchedEffect(stopId) {
        stopId?.let { id ->
            viewModel.selectStop(de.vvo.glassapp.data.model.Stop(id, "", null, null, null))
        }
    }

    LaunchedEffect(selectedStop) {
        selectedStop?.let { stop ->
            if (stop.lat != null && stop.lon != null) {
                mapInstance?.animateCamera(
                    com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
                        LatLng(stop.lat, stop.lon), 15.0
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            Mapbox.getInstance(context)
        } catch (e: Exception) { }
    }

    // Refresh map data periodically based on current viewport
    LaunchedEffect(mapInstance) {
        while(true) {
            mapInstance?.let { map ->
                val bounds = map.projection.visibleRegion.latLngBounds
                viewModel.loadMapData(bounds.getLatSouth(), bounds.getLonWest(), bounds.getLatNorth(), bounds.getLonEast())
            } ?: run {
                // Fallback to initial Dresden center if map not ready
                viewModel.loadMapData(51.0, 13.6, 51.1, 13.9)
            }
            delay(10000)
        }
    }

    // Update vehicle symbols
    LaunchedEffect(pins, vehicleManager) {
        vehicleManager?.let { manager ->
            try {
                android.util.Log.d("MapScreen", "Updating vehicle symbols: ${pins.size}")
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
                        .withTextSize(12f)
                        .withTextColor("#000000")
                        .withTextHaloColor("#FFFFFF")
                        .withTextHaloWidth(2.0f)
                        .withIconImage(icon)
                        .withIconColor(color)
                        .withIconSize(1.2f)
                        .withData(com.google.gson.JsonPrimitive("v_${pin.id}"))
                    )
                }
            } catch (e: Exception) { }
        }
    }

    // POI Marker
    LaunchedEffect(lat, lon, poiManager) {
        poiManager?.let { manager ->
            manager.deleteAll()
            if (lat != null && lon != null && stopId == null) {
                manager.create(SymbolOptions()
                    .withLatLng(LatLng(lat, lon))
                    .withIconImage("marker-15")
                    .withIconColor("#007AFF")
                    .withIconSize(1.5f)
                )
            }
        }
    }

    // Update stop symbols
    LaunchedEffect(stops, stopManager) {
        stopManager?.let { manager ->
            try {
                android.util.Log.d("MapScreen", "Updating stop symbols: ${stops.size}")
                manager.deleteAll()
                stops.forEach { stop ->
                    if (stop.lat != null && stop.lon != null) {
                        manager.create(SymbolOptions()
                            .withLatLng(LatLng(stop.lat, stop.lon))
                            .withIconImage("dot-11")
                            .withIconColor("#FFFFFF")
                            .withIconSize(1.5f)
                            .withData(com.google.gson.JsonPrimitive("s_${stop.id}"))
                        )
                    }
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
        val mapView = remember { MapView(context) }

        DisposableEffect(mapView) {
            mapView.onCreate(null)
            mapView.onStart()
            mapView.onResume()
            onDispose {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }

        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { mapboxMap ->
                        mapInstance = mapboxMap
                        val styleUrl = "https://tiles.openfreemap.org/styles/bright"
                        mapboxMap.setStyle(styleUrl) { style ->
                            // Add essential icons to style if missing
                            fun createCircleBitmap(size: Int, color: Int, strokeColor: Int = android.graphics.Color.BLACK): android.graphics.Bitmap {
                                val b = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                                val c = android.graphics.Canvas(b)
                                val p = android.graphics.Paint()
                                p.isAntiAlias = true
                                p.color = color
                                p.style = android.graphics.Paint.Style.FILL
                                c.drawCircle(size/2f, size/2f, size/2f - 2f, p)
                                p.color = strokeColor
                                p.style = android.graphics.Paint.Style.STROKE
                                p.strokeWidth = 2f
                                c.drawCircle(size/2f, size/2f, size/2f - 2f, p)
                                return b
                            }

                            style.addImage("tram", createCircleBitmap(40, android.graphics.Color.parseColor("#FFCC00")))
                            style.addImage("bus", createCircleBitmap(40, android.graphics.Color.parseColor("#FFCC00")))
                            style.addImage("rail", createCircleBitmap(40, android.graphics.Color.parseColor("#FFCC00")))
                            style.addImage("marker-15", createCircleBitmap(32, android.graphics.Color.RED))
                            style.addImage("dot-11", createCircleBitmap(16, android.graphics.Color.WHITE))

                            vehicleManager = SymbolManager(this, mapboxMap, style).apply {
                                iconAllowOverlap = true
                                textAllowOverlap = true
                                addClickListener { symbol ->
                                    val data = symbol.data?.asString
                                    if (data?.startsWith("v_") == true) {
                                        val vehicleId = data.removePrefix("v_")
                                        pins.find { it.id == vehicleId }?.let { viewModel.selectVehicle(it) }
                                    }
                                    true
                                }
                            }
                            stopManager = SymbolManager(this, mapboxMap, style).apply {
                                iconAllowOverlap = true
                                addClickListener { symbol ->
                                    val data = symbol.data?.asString
                                    if (data?.startsWith("s_") == true) {
                                        val foundStopId = data.removePrefix("s_")
                                        stops.find { it.id == foundStopId }?.let { viewModel.selectStop(it) }
                                    }
                                    true
                                }
                            }
                            poiManager = SymbolManager(this, mapboxMap, style)
                            lineManager = LineManager(this, mapboxMap, style)

                            mapboxMap.addOnMapClickListener {
                                viewModel.deselectAll()
                                true
                            }

                            try {
                                val locationComponent = mapboxMap.locationComponent
                                locationComponent.activateLocationComponent(
                                    com.mapbox.mapboxsdk.location.LocationComponentActivationOptions.builder(context, style).build()
                                )
                                locationComponent.isLocationComponentEnabled = true
                                locationComponent.renderMode = com.mapbox.mapboxsdk.location.modes.RenderMode.COMPASS
                            } catch (e: Exception) { }
                        }
                        val target = if (lat != null && lon != null) {
                            LatLng(lat, lon)
                        } else if (userLocationState != null) {
                            userLocationState!!
                        } else {
                            LatLng(51.0509, 13.7373)
                        }

                        mapboxMap.cameraPosition = CameraPosition.Builder()
                            .target(target)
                            .zoom(if (lat != null || userLocationState != null) 15.0 else 13.0)
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
                val target = userLocationState ?: LatLng(51.0509, 13.7373)
                mapInstance?.animateCamera(
                    com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
                        target, 15.0
                    )
                )
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
                RouteDetailSheet(stops = routeStops, line = selectedVehicle?.line, onStopClick = { stop ->
                    mapInstance?.animateCamera(
                        com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
                            LatLng(stop.lat, stop.lon), 15.0
                        )
                    )
                })
                IconButton(
                    onClick = { viewModel.deselectAll() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        } else if (selectedStop != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding()
            ) {
                StopDetailSheet(stop = selectedStop!!, departures = departures)
                IconButton(
                    onClick = { viewModel.deselectAll() },
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
