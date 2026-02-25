package de.vvo.glassapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.vvo.glassapp.R
import de.vvo.glassapp.data.model.VehiclePin
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.components.LocalGlassBackdrop
import de.vvo.glassapp.ui.components.RouteDetailSheet
import de.vvo.glassapp.ui.components.StopDetailSheet
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalGraphicsContext
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import org.maplibre.compose.camera.CameraPosition as MapCameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.*
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

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

@SuppressLint("MissingPermission")
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
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        fun isInGermany(lat: Double, lon: Double) = lat in 47.0..56.0 && lon in 5.0..16.0
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            if (location != null && isInGermany(location.latitude, location.longitude)) {
                viewModel.updateUserLocation(location.latitude, location.longitude)
            } else {
                viewModel.updateUserLocation(51.0509, 13.7373)
            }
        } catch (e: SecurityException) {
            viewModel.updateUserLocation(51.0509, 13.7373)
        }
    }

    val pins by viewModel.mapPins.collectAsState()
    val stops by viewModel.mapStops.collectAsState()
    val departures by viewModel.departures.collectAsState()
    val userLocationState by viewModel.userLocation.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val selectedStop by viewModel.selectedStop.collectAsState()
    val routeStops by viewModel.selectedVehicleRoute.collectAsState()
    val currentTripRoute by viewModel.currentTripRoute.collectAsState()
    val selectedTrip by viewModel.selectedTrip.collectAsState()

    // Camera state – initial position: given lat/lon, user location, or Dresden center
    val initialTarget = when {
        lat != null && lon != null -> Position(lon, lat)
        userLocationState != null -> Position(userLocationState!!.longitude, userLocationState!!.latitude)
        else -> Position(13.7373, 51.0509)
    }
    val initialZoom = if (lat != null || userLocationState != null) 15.0 else 13.0
    val mapCameraState = rememberCameraState(
        firstPosition = MapCameraPosition(target = initialTarget, zoom = initialZoom)
    )

    var hasInitialCentered by remember { mutableStateOf(false) }
    LaunchedEffect(userLocationState) {
        if (!hasInitialCentered && userLocationState != null && stopId == null && lat == null) {
            mapCameraState.animateTo(
                MapCameraPosition(
                    target = Position(userLocationState!!.longitude, userLocationState!!.latitude),
                    zoom = 15.0
                )
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
                mapCameraState.animateTo(
                    MapCameraPosition(target = Position(sLon, sLat), zoom = 15.0)
                )
            }
        }
    }

    // Fit camera to trip route bounding box when route is loaded
    LaunchedEffect(currentTripRoute) {
        if (currentTripRoute.size > 1) {
            val minLat = currentTripRoute.minOf { it.latitude }
            val maxLat = currentTripRoute.maxOf { it.latitude }
            val minLon = currentTripRoute.minOf { it.longitude }
            val maxLon = currentTripRoute.maxOf { it.longitude }
            mapCameraState.animateTo(BoundingBox(minLon, minLat, maxLon, maxLat))
        }
    }

    // Periodic map data refresh based on current camera position
    LaunchedEffect(Unit) {
        while (true) {
            val pos = mapCameraState.position.target
            viewModel.loadMapData(
                pos.latitude - 0.05,
                pos.longitude - 0.1,
                pos.latitude + 0.05,
                pos.longitude + 0.1
            )
            delay(10000)
        }
    }

    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) { onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) } }
    val backdrop = rememberLayerBackdrop(graphicsLayer)

    Box(modifier = Modifier.fillMaxSize()) {

        MaplibreMap(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/bright"),
            cameraState = mapCameraState,
            options = MapOptions(
                renderOptions = RenderOptions(renderMode = RenderOptions.RenderMode.TextureView)
            )
        ) {
            // ── Vehicle layer ──────────────────────────────────────────────
            val vehicleSource = rememberGeoJsonSource(remember(pins) {
                val featuresJson = pins.joinToString(",") { pin ->
                    val lon = pin.longitudeValue()
                    val lat = pin.latitudeValue()
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[$lon,$lat]},"properties":{"id":${JsonPrimitive(pin.id)},"line":${JsonPrimitive(pin.line)},"punctuality":${pin.punctuality ?: 0}}}"""
                }
                GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$featuresJson]}""")
            })

            CircleLayer(
                id = "vehicle-circles",
                source = vehicleSource,
                radius = const(18.dp),
                color = switch(
                    condition(
                        test = feature["punctuality"].asNumber() lt const(0f),
                        output = const(Color(0xFF4CAF50))
                    ),
                    condition(
                        test = feature["punctuality"].asNumber() gt const(0f),
                        output = const(Color(0xFFF44336))
                    ),
                    fallback = const(Color(0xFFFFCC00))
                ),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { features ->
                    val id = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content
                    val pin = pins.find { it.id == id }
                    if (pin != null) { viewModel.selectVehicle(pin); ClickResult.Consume }
                    else ClickResult.Pass
                }
            )

            SymbolLayer(
                id = "vehicle-labels",
                source = vehicleSource,
                textField = format(span(feature["line"].asString())),
                textColor = const(Color.White),
                textSize = const(12.sp),
                textAllowOverlap = const(true),
                iconAllowOverlap = const(true)
            )

            // ── Stop layer ─────────────────────────────────────────────────
            val stopSource = rememberGeoJsonSource(remember(stops) {
                val featuresJson = stops.mapNotNull { stop ->
                    val sLat = stop.latitudeValue() ?: return@mapNotNull null
                    val sLon = stop.longitudeValue() ?: return@mapNotNull null
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[$sLon,$sLat]},"properties":{"id":${JsonPrimitive(stop.id)}}}"""
                }.joinToString(",")
                GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$featuresJson]}""")
            })

            CircleLayer(
                id = "stop-circles",
                source = stopSource,
                radius = const(6.dp),
                color = const(Color.White),
                strokeColor = const(Color(0xFF333333)),
                strokeWidth = const(1.5.dp),
                onClick = { features ->
                    val id = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content
                    val stop = stops.find { it.id == id }
                    if (stop != null) { viewModel.selectStop(stop); ClickResult.Consume }
                    else ClickResult.Pass
                }
            )

            // ── Route line ─────────────────────────────────────────────────
            val isVehicleRoute = routeStops.isNotEmpty()
            val routeCoordinates = remember(routeStops, currentTripRoute) {
                when {
                    routeStops.isNotEmpty() -> routeStops.mapNotNull { stop ->
                        val rLat = stop.latitudeValue() ?: return@mapNotNull null
                        val rLon = stop.longitudeValue() ?: return@mapNotNull null
                        Position(rLon, rLat)
                    }
                    currentTripRoute.isNotEmpty() ->
                        currentTripRoute.map { Position(it.longitude, it.latitude) }
                    else -> emptyList()
                }
            }
            val routeSource = rememberGeoJsonSource(remember(routeCoordinates) {
                if (routeCoordinates.size >= 2) {
                    val coords = routeCoordinates.joinToString(",") { "[${it.longitude},${it.latitude}]" }
                    GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]},"properties":{}}]}""")
                } else {
                    GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}""")
                }
            })

            LineLayer(
                id = "route-line",
                source = routeSource,
                color = if (isVehicleRoute) const(Color(0xFFFFCC00)) else const(Color(0xFF007AFF)),
                width = if (isVehicleRoute) const(4.dp) else const(6.dp),
                opacity = const(0.8f),
                visible = routeCoordinates.size >= 2
            )

            // ── POI markers ────────────────────────────────────────────────
            val poiSource = rememberGeoJsonSource(remember(lat, lon, currentTripRoute) {
                val featuresList = mutableListOf<String>()
                if (lat != null && lon != null && stopId == null) {
                    featuresList += """{"type":"Feature","geometry":{"type":"Point","coordinates":[$lon,$lat]},"properties":{"type":"poi"}}"""
                }
                if (currentTripRoute.size >= 2) {
                    val start = currentTripRoute.first()
                    val end = currentTripRoute.last()
                    featuresList += """{"type":"Feature","geometry":{"type":"Point","coordinates":[${start.longitude},${start.latitude}]},"properties":{"type":"start"}}"""
                    featuresList += """{"type":"Feature","geometry":{"type":"Point","coordinates":[${end.longitude},${end.latitude}]},"properties":{"type":"end"}}"""
                }
                GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[${featuresList.joinToString(",")}]}""")
            })

            CircleLayer(
                id = "poi-circles",
                source = poiSource,
                radius = const(10.dp),
                color = switch(
                    condition(
                        test = feature["type"].asString() eq const("start"),
                        output = const(Color(0xFF4CAF50))
                    ),
                    condition(
                        test = feature["type"].asString() eq const("end"),
                        output = const(Color(0xFFF44336))
                    ),
                    fallback = const(Color(0xFF007AFF))
                ),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                visible = (lat != null && lon != null && stopId == null) || currentTripRoute.size >= 2
            )

            // ── User location dot ──────────────────────────────────────────
            val userLocSource = rememberGeoJsonSource(remember(userLocationState) {
                val loc = userLocationState
                if (loc != null) {
                    GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${loc.longitude},${loc.latitude}]},"properties":{}}]}""")
                } else {
                    GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}""")
                }
            })
            CircleLayer(
                id = "user-location-dot",
                source = userLocSource,
                radius = const(8.dp),
                color = const(Color(0xFF007AFF)),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )
        }

        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {

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
                val targetPos = userLocationState?.let { Position(it.longitude, it.latitude) }
                    ?: Position(13.7373, 51.0509)
                coroutineScope.launch {
                    mapCameraState.animateTo(MapCameraPosition(target = targetPos, zoom = 15.0))
                }
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

        if (selectedTrip != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding()
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Verbindung", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = DvbYellow, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${selectedTrip!!.departureTime} - ${selectedTrip!!.arrivalTime}", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${selectedTrip!!.duration} min", color = DvbYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                IconButton(
                    onClick = { viewModel.deselectAll() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        } else if (selectedVehicle != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding()
            ) {
                RouteDetailSheet(stops = routeStops, line = selectedVehicle?.line, onStopClick = { stop ->
                    val sLat = stop.latitudeValue() ?: return@RouteDetailSheet
                    val sLon = stop.longitudeValue() ?: return@RouteDetailSheet
                    coroutineScope.launch {
                        mapCameraState.animateTo(MapCameraPosition(target = Position(sLon, sLat), zoom = 15.0))
                    }
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
        } // CompositionLocalProvider
    }
}
