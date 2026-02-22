package de.vvo.glassapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.vvo.glassapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import de.vvo.glassapp.data.model.Stop
import com.mapbox.mapboxsdk.camera.CameraPosition
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.viewmodel.TransitViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: TransitViewModel = viewModel(factory = TransitViewModel.Factory)
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchResults by viewModel.searchResults.collectAsState()
    val locationResults by viewModel.locationResults.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val isSearchingTrips = viewModel.isSearchingTrips
    val favorites by viewModel.favorites.collectAsState()
    val customOrigin by viewModel.customOrigin.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var showEditFavoriteDialog by remember { mutableStateOf<de.vvo.glassapp.ui.viewmodel.Favorite?>(null) }
    var favoriteNewName by remember { mutableStateOf("") }
    var isArrivalMode by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var timeInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    var isSearchingOrigin by remember { mutableStateOf(false) }
    var originQuery by remember { mutableStateOf("") }
    val originSearchResults by viewModel.searchResults.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshLocation(context)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            kotlinx.coroutines.delay(300)
            viewModel.searchStops(searchQuery)
        } else if (searchQuery.isEmpty()) {
            viewModel.searchStops("")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Background
        val mapView = remember { com.mapbox.mapboxsdk.maps.MapView(context) }
        androidx.compose.runtime.DisposableEffect(mapView) {
            mapView.onCreate(null)
            mapView.onStart()
            mapView.onResume()
            onDispose {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }
        var mapInstance by remember { mutableStateOf<com.mapbox.mapboxsdk.maps.MapboxMap?>(null) }
        LaunchedEffect(userLocation) {
            userLocation?.let { loc ->
                mapInstance?.animateCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng(loc))
            }
        }

        androidx.compose.ui.viewinterop.AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapInstance = map
                        val styleUrl = "https://tiles.openfreemap.org/styles/bright"
                        map.setStyle(styleUrl)

                        // Disable all gestures
                        map.uiSettings.isScrollGesturesEnabled = false
                        map.uiSettings.isZoomGesturesEnabled = false
                        map.uiSettings.isTiltGesturesEnabled = false
                        map.uiSettings.isRotateGesturesEnabled = false
                        map.uiSettings.isDoubleTapGesturesEnabled = false

                        val target = userLocation ?: com.mapbox.mapboxsdk.geometry.LatLng(51.0509, 13.7373)
                        map.cameraPosition = com.mapbox.mapboxsdk.camera.CameraPosition.Builder()
                            .target(target)
                            .zoom(14.0)
                            .build()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay to improve UI contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Clean Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DVB-Smart",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.5).sp
            )
            Text(
                text = "Dein Weg durch Dresden.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.search_hint),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1200, easing = LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("assistant")
                },
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationY = offsetY
                )
            ) {
                GlassCard(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = DvbYellow,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Connection Search Panel
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(24.dp),
            padding = 16.dp
        ) {
            Column {
                // Origin
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (customOrigin == null) Icons.Default.MyLocation else Icons.Default.Place,
                        contentDescription = null,
                        tint = DvbYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (originQuery.isEmpty() && customOrigin == null) {
                            Text("Mein Standort (oder Start suchen...)", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                        }
                        BasicTextField(
                            value = if (customOrigin != null && originQuery.isEmpty()) customOrigin!!.name else originQuery,
                            onValueChange = {
                                originQuery = it
                                isSearchingOrigin = it.isNotEmpty()
                                if (it.isNotEmpty()) viewModel.searchStops(it)
                            },
                            textStyle = TextStyle(
                                color = if (customOrigin != null && originQuery.isEmpty()) DvbYellow else Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (customOrigin != null) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (customOrigin != null || originQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.setCustomOrigin(null)
                            originQuery = ""
                            isSearchingOrigin = false
                        }, modifier = Modifier.size(24.dp)) {
                            Text("✕", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                // Time Selection
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    showTimeDialog = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DvbYellow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isArrivalMode) "Ankunft" else "Abfahrt",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        selectedTime ?: "Jetzt",
                        color = DvbYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                // Destination / Search
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text("Ziel suchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isSearchingOrigin = false
                            },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.searchStops(searchQuery)
                                    keyboardController?.hide()
                                }
                            })
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = searchQuery.isNotEmpty() || originQuery.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SectionHeader(if (isSearchingOrigin) "Start wählen" else "Haltestellen")
                }
                val results = if (isSearchingOrigin) originSearchResults else searchResults
                items(results) { stop ->
                    GlassSearchResultItem(
                        title = stop.name,
                        subtitle = stop.place ?: "Dresden",
                        isFavorite = favorites.any { it.stopId == stop.id },
                        onFavoriteClick = { viewModel.toggleFavorite(stop.name, stop.id) }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isSearchingOrigin) {
                            viewModel.setCustomOrigin(stop)
                            originQuery = ""
                            isSearchingOrigin = false
                        } else {
                            if (customOrigin == null) {
                                viewModel.selectStop(stop)
                                navController.navigate("map/${stop.id}")
                            } else {
                                viewModel.findTrips(customOrigin!!.id, stop.id, selectedTime, isArrivalMode)
                                searchQuery = ""
                            }
                        }
                    }
                }
                item {
                    SectionHeader("Adressen & Orte")
                }
                items(locationResults) { feature ->
                    GlassSearchResultItem(
                        title = feature.properties.name,
                        subtitle = feature.properties.city ?: "Dresden",
                        icon = Icons.Default.Place
                    ) {
                        val coords = feature.geometry.coordinates
                        if (coords.size >= 2) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val stop = de.vvo.glassapp.data.model.Stop(
                                id = "coord:${coords[0]}:${coords[1]}",
                                name = feature.properties.name,
                                place = feature.properties.city,
                                lat = coords[1],
                                lon = coords[0]
                            )
                            if (isSearchingOrigin) {
                                viewModel.setCustomOrigin(stop)
                                originQuery = ""
                                isSearchingOrigin = false
                            } else {
                                if (customOrigin == null) {
                                    navController.navigate("map/${coords[1]}/${coords[0]}")
                                } else {
                                    viewModel.findTrips(customOrigin!!.id, stop.id, selectedTime, isArrivalMode)
                                    searchQuery = ""
                                }
                            }
                        }
                    }
                }
            }
        }

        if (searchQuery.isEmpty()) {
            // Favorites Section
            SectionHeader(stringResource(R.string.favorites))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                items(favorites) { favorite ->
                    FavoriteItem(
                        favorite = favorite,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.findTrips(null, favorite.stopId, selectedTime, isArrivalMode)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            favoriteNewName = favorite.name
                            showEditFavoriteDialog = favorite
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isSearchingTrips,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DvbYellow)
                }
            }

            AnimatedVisibility(
                visible = trips.isNotEmpty() && !isSearchingTrips,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    SectionHeader(stringResource(R.string.connections))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(trips) { trip ->
                        TripItem(trip, navController)
                        }
                    }
                }
            }

            if (trips.isEmpty() && !isSearchingTrips) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                navController.navigate("map")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DvbYellow),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.to_map), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    }

    // Time Settings Sheet
    if (showTimeDialog) {
        ModalBottomSheet(
            onDismissRequest = { showTimeDialog = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(32.dp),
                padding = 24.dp
            ) {
                Column {
                    Text("Reisezeit planen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isArrivalMode = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isArrivalMode) DvbYellow else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Abfahrt", color = if (!isArrivalMode) Color.Black else Color.White)
                        }
                        Button(
                            onClick = { isArrivalMode = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isArrivalMode) DvbYellow else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Ankunft", color = if (isArrivalMode) Color.Black else Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Zeit (z.B. 14:30)", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DvbYellow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (timeInput.contains(":")) {
                                selectedTime = timeInput
                            } else {
                                selectedTime = null
                            }
                            showTimeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DvbYellow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Fertig", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    TextButton(
                        onClick = {
                            selectedTime = null
                            timeInput = ""
                            showTimeDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Jetzt abfahren", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    // Edit Favorite Dialog
    val currentEditingFavorite = favorites.find { it.stopId == showEditFavoriteDialog?.stopId }
    if (showEditFavoriteDialog != null && currentEditingFavorite != null) {
        AlertDialog(
            onDismissRequest = { showEditFavoriteDialog = null },
            title = { Text("Favorit bearbeiten") },
            text = {
                Column {
                    OutlinedTextField(
                        value = favoriteNewName,
                        onValueChange = { favoriteNewName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.Black)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Icon wählen", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val icons = listOf("Star", "Home", "Work", "School", "Train", "Place")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        icons.forEach { iconName ->
                            val iconVector = when(iconName) {
                                "Work" -> Icons.Default.Work
                                "Home" -> Icons.Default.Home
                                "School" -> Icons.Default.School
                                "Train" -> Icons.Default.Train
                                "Place" -> Icons.Default.Place
                                else -> Icons.Filled.Star
                            }
                            IconButton(
                                onClick = { viewModel.updateFavoriteIcon(currentEditingFavorite.stopId, iconName) },
                                modifier = Modifier.size(40.dp).background(
                                    if (currentEditingFavorite.iconName == iconName) DvbYellow.copy(alpha = 0.2f) else Color.Transparent,
                                    CircleShape
                                )
                            ) {
                                Icon(iconVector, contentDescription = null, tint = if (currentEditingFavorite.iconName == iconName) DvbYellow else Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.toggleFavorite(showEditFavoriteDialog!!.name, showEditFavoriteDialog!!.stopId)
                            showEditFavoriteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Löschen", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFavoriteName(showEditFavoriteDialog!!.stopId, favoriteNewName)
                    showEditFavoriteDialog = null
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFavoriteDialog = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 14.dp, top = 8.dp)
    )
}

@Composable
fun GlassSearchResultItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.DirectionsWalk,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DvbYellow,
                modifier = Modifier.size(24.dp).padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
            if (onFavoriteClick != null) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) DvbYellow else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteItem(
    favorite: de.vvo.glassapp.ui.viewmodel.Favorite,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        padding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                val icon = when(favorite.iconName) {
                    "Work" -> Icons.Default.Work
                    "Home" -> Icons.Default.Home
                    "School" -> Icons.Default.School
                    "Train" -> Icons.Default.Train
                    "Place" -> Icons.Default.Place
                    else -> Icons.Filled.Star
                }
                Surface(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = DvbYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = favorite.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TripItem(trip: de.vvo.glassapp.data.model.Trip, navController: NavController) {
    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "${trip.departureTime} - ${trip.arrivalTime}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                    Text(text = "Dauer: ${trip.duration} min", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Text(text = if (trip.interchanges == 0) "Direkt" else "${trip.interchanges} Umstiege", fontSize = 13.sp, color = DvbYellow, fontWeight = FontWeight.Bold)
            }
            if (trip.sections != null && trip.sections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trip.sections.forEachIndexed { index, section ->
                        if (section.line != null) {
                            Surface(
                                color = DvbYellow,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigate("map")
                                    }
                            ) {
                                Text(
                                    text = section.line!!,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (section.type == "Footway" || section.type == "Walking") {
                             Icon(
                                Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp).padding(end = 2.dp)
                             )
                             Text(
                                "${section.duration ?: 0}m",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(end = 4.dp)
                             )
                        }
                        if (index < trip.sections.size - 1) {
                            Text(">", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
