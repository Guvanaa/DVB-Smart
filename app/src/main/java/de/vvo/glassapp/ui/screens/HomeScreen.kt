package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.vvo.glassapp.R
import de.vvo.glassapp.data.model.Stop
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.viewmodel.TransitViewModel
import de.vvo.glassapp.util.ServiceLocator

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: TransitViewModel = remember { TransitViewModel(ServiceLocator.repository) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val locationResults by viewModel.locationResults.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "DVB Glass",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Custom Glass Search Bar
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(12.dp))
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length > 2) viewModel.searchStops(it)
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (searchQuery.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    SectionHeader("Haltestellen")
                }
                items(searchResults) { stop ->
                    GlassSearchResultItem(stop.name, stop.place ?: "Dresden") {
                        navController.navigate("map/${stop.id}")
                    }
                }
                item {
                    SectionHeader("Adressen & Orte")
                }
                items(locationResults) { feature ->
                    GlassSearchResultItem(feature.properties.name, feature.properties.city ?: "") {
                        val coords = feature.geometry.coordinates
                        if (coords.size >= 2) {
                            navController.navigate("map/${coords[1]}/${coords[0]}")
                        } else {
                            navController.navigate("map")
                        }
                    }
                }
            }
        } else {
            // Favorites Section
            SectionHeader(stringResource(R.string.favorites))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                items(favorites) { favorite ->
                    FavoriteItem(favorite) {
                        viewModel.findTrips("33000028", favorite.stopId)
                    }
                }
            }

            if (trips.isNotEmpty()) {
                SectionHeader(stringResource(R.string.connections))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(trips) { trip ->
                        TripItem(trip)
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Button(
            onClick = { navController.navigate("map") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DvbYellow),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Zur Karte", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
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
fun GlassSearchResultItem(title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun FavoriteItem(favorite: de.vvo.glassapp.ui.viewmodel.Favorite, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .size(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = DvbYellow, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = favorite.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun TripItem(trip: de.vvo.glassapp.data.model.Trip) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "${trip.departureTime} - ${trip.arrivalTime}", fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Dauer: ${trip.duration} min", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Text(text = "${trip.interchanges} Umstiege", fontSize = 13.sp, color = DvbYellow, fontWeight = FontWeight.Bold)
        }
    }
}
