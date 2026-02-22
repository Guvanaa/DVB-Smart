package de.vvo.glassapp.ui.screens

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import de.vvo.glassapp.ui.components.*
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import kotlinx.coroutines.delay

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun VehicleListSheet(
    pins: List<VehiclePin>,
    onVehicleClick: (VehiclePin) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pins) { pin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleClick(pin) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when {
                        (pin.punctuality ?: 0) < 0 -> Color(0xFF4CAF50)
                        (pin.punctuality ?: 0) == 0 -> Color(0xFFFFCC00)
                        else -> Color(0xFFF44336)
                    }

                    Box(
                        modifier = Modifier.background(statusColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(pin.line, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pin.direction, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        val delay = pin.punctuality ?: 0
                        val delayText = if (delay > 0) "+$delay min" else if (delay < 0) "$delay min" else "Pünktlich"
                        Text(delayText, color = if (delay > 0) Color(0xFFF44336) else if (delay < 0) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

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
        // Try to get real location
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            location?.let {
                viewModel.updateUserLocation(it.latitude, it.longitude)
            } ?: run {
                // Fallback to center if no location found yet
                viewModel.updateUserLocation(51.0509, 13.7373)
            }
        } catch (e: SecurityException) {
            viewModel.updateUserLocation(51.0509, 13.7373)
        }
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

    var hasInitialCentered by remember { mutableStateOf(false) }
    LaunchedEffect(userLocationState) {
        if (!hasInitialCentered && userLocationState != null && stopId == null && lat == null) {
            mapInstance?.animateCamera(
                com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(userLocationState!!, 15.0)
            )
            hasInitialCentered = true
        }
    }

    LaunchedEffect(stopId) {
        stopId?.let { id ->
            viewModel.selectStop(de.vvo.glassapp.data.model.Stop(id, "", null, null, null))
        }
    }

    LaunchedEffect(selectedStop) {
        selectedStop?.let { stop ->
            val sLat = stop.latitudeValue()
            val sLon = stop.longitudeValue()
            if (sLat != null && sLon != null) {
                mapInstance?.animateCamera(
                    com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(
                        LatLng(sLat, sLon), 15.0
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
        val manager = vehicleManager ?: return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        map.getStyle { style ->
            try {
                manager.deleteAll()
                pins.forEach { pin ->
                    val statusColor = when {
                        (pin.punctuality ?: 0) < 0 -> "#4CAF50"
                        (pin.punctuality ?: 0) == 0 -> "#FFCC00"
                        else -> "#F44336"
                    }

                    val imageId = "icon_${pin.line}_${statusColor}"
                    if (style.getImage(imageId) == null) {
                        val size = 70
                        val b = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                        val c = android.graphics.Canvas(b)
                        val p = android.graphics.Paint()
                        p.isAntiAlias = true
                        p.color = android.graphics.Color.parseColor(statusColor)
                        c.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 15f, 15f, p)

                        p.color = android.graphics.Color.WHITE
                        p.textSize = 28f
                        p.textAlign = android.graphics.Paint.Align.CENTER
                        p.isFakeBoldText = true
                        val xPos = size / 2f
                        val yPos = (size / 2f - (p.descent() + p.ascent()) / 2f)
                        c.drawText(pin.line, xPos, yPos, p)
                        style.addImage(imageId, b)
                    }

                    manager.create(SymbolOptions()
                        .withLatLng(LatLng(pin.latitudeValue(), pin.longitudeValue()))
                        .withIconImage(imageId)
                        .withIconSize(1.0f)
                        .withData(com.google.gson.JsonPrimitive("v_${pin.id}"))
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Error updating vehicle symbols", e)
            }
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
                    val sLat = stop.latitudeValue()
                    val sLon = stop.longitudeValue()
                    if (sLat != null && sLon != null) {
                        manager.create(SymbolOptions()
                            .withLatLng(LatLng(sLat, sLon))
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
                val points = routeStops.map { LatLng(it.latitudeValue(), it.longitudeValue()) }
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

                            style.addImage("v_green", createCircleBitmap(44, android.graphics.Color.parseColor("#4CAF50")))
                            style.addImage("v_yellow", createCircleBitmap(44, android.graphics.Color.parseColor("#FFCC00")))
                            style.addImage("v_red", createCircleBitmap(44, android.graphics.Color.parseColor("#F44336")))
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
                viewModel.refreshLocation(context)
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
                            LatLng(stop.latitudeValue(), stop.longitudeValue()), 15.0
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
            var showVehicleList by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).navigationBarsPadding()
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    onClick = { showVehicleList = !showVehicleList }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.live_traffic), fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                            Spacer(Modifier.weight(1f))
                            if (showVehicleList) Text("✕", color = Color.White.copy(alpha = 0.6f))
                        }
                        Text(stringResource(R.string.vehicles_nearby, pins.size), color = Color.White.copy(alpha = 0.8f))
                    }
                }

                if (showVehicleList) {
                    VehicleListSheet(
                        pins = pins,
                        onVehicleClick = { pin ->
                            viewModel.selectVehicle(pin)
                            showVehicleList = false
                        },
                        modifier = Modifier.padding(bottom = 100.dp)
                    )
                }
            }
        }
    }
}
