package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: TransitViewModel = remember { TransitViewModel(ServiceLocator.repository) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val locationResults by viewModel.locationResults.collectAsState()
    val trips by viewModel.trips.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "DVB Glass",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DvbYellow,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.length > 2) viewModel.searchStops(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = DvbYellow,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
            )
        )

        if (searchQuery.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { Text("Haltestellen", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                items(searchResults) { stop ->
                    StopSearchResultItem(stop) {
                        navController.navigate("map/${stop.id}")
                    }
                }
                item { Text("Adressen & Orte", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                items(locationResults) { feature ->
                    LocationSearchResultItem(feature) {
                        // Navigate to map at these coordinates
                        navController.navigate("map")
                    }
                }
            }
        } else {
            // Favorites Section
            Text(
                text = stringResource(R.string.favorites),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(sampleFavorites) { favorite ->
                    FavoriteItem(favorite) {
                        // Search for connections from Dresden Hbf (mock current location) to favorite
                        viewModel.findTrips("33000028", favorite.stopId)
                    }
                }
            }

            if (trips.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.connections),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(trips) { trip ->
                        TripItem(trip)
                    }
                }
            }

            // Nearby Departures
            Text(
                text = stringResource(R.string.departures),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Placeholder for departures from current location or default stop
            GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Text("Lade Abfahrten in der Nähe...", modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("map") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DvbYellow)
        ) {
            Text("Zur Karte", color = Color.Black)
        }
    }
}

@Composable
fun LocationSearchResultItem(feature: de.vvo.glassapp.data.model.PhotonFeature, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = feature.properties.name, fontWeight = FontWeight.Bold)
            Text(text = "${feature.properties.city ?: ""}, ${feature.properties.street ?: ""}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StopSearchResultItem(stop: Stop, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stop.name, fontWeight = FontWeight.Bold)
            Text(text = stop.place ?: "Dresden", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TripItem(trip: de.vvo.glassapp.data.model.Trip) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "${trip.departureTime} - ${trip.arrivalTime}", fontWeight = FontWeight.Bold)
                Text(text = "Dauer: ${trip.duration} min", fontSize = 12.sp)
            }
            Text(text = "${trip.interchanges} Umstiege", fontSize = 12.sp)
        }
    }
}

@Composable
fun FavoriteItem(favorite: Favorite, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = DvbYellow)
            Text(text = favorite.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

data class Favorite(val name: String, val stopId: String)
val sampleFavorites = listOf(
    Favorite("Arbeit", "33000028"),
    Favorite("Zuhause", "33000037"),
    Favorite("Hbf", "33000028")
)
