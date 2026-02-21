package de.vvo.glassapp.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.vvo.glassapp.util.ServiceLocator
import com.mapbox.mapboxsdk.geometry.LatLng
import de.vvo.glassapp.data.model.*
import de.vvo.glassapp.data.repository.FavoritesManager
import de.vvo.glassapp.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Favorite(val name: String, val stopId: String)

class TransitViewModel(
    private val repository: TransitRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TransitViewModel(ServiceLocator.repository, ServiceLocator.favoritesManager)
            }
        }
    }
    private val TAG = "TransitViewModel"

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                _favorites.value = favoritesManager.getFavorites()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load favorites", e)
            }
        }
    }

    private val _searchResults = MutableStateFlow<List<Stop>>(emptyList())
    val searchResults: StateFlow<List<Stop>> = _searchResults

    private val _locationResults = MutableStateFlow<List<PhotonFeature>>(emptyList())
    val locationResults: StateFlow<List<PhotonFeature>> = _locationResults

    private val _departures = MutableStateFlow<List<Departure>>(emptyList())
    val departures: StateFlow<List<Departure>> = _departures

    private val _mapPins = MutableStateFlow<List<VehiclePin>>(emptyList())
    val mapPins: StateFlow<List<VehiclePin>> = _mapPins

    private val _mapStops = MutableStateFlow<List<Stop>>(emptyList())
    val mapStops: StateFlow<List<Stop>> = _mapStops

    private val _selectedStop = MutableStateFlow<Stop?>(null)
    val selectedStop: StateFlow<Stop?> = _selectedStop

    private val _selectedVehicleRoute = MutableStateFlow<List<StopPoint>>(emptyList())
    val selectedVehicleRoute: StateFlow<List<StopPoint>> = _selectedVehicleRoute

    private val _selectedVehicle = MutableStateFlow<VehiclePin?>(null)
    val selectedVehicle: StateFlow<VehiclePin?> = _selectedVehicle

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    var isSearchingTrips by mutableStateOf(false)

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = LatLng(lat, lon)
    }

    val assistantMessages = mutableStateListOf<ChatMessage>()
    var isAssistantTyping by mutableStateOf(false)

    fun sendMessageToAssistant(text: String, context: android.content.Context) {
        if (text.isBlank()) return
        assistantMessages.add(ChatMessage(text, true))

        viewModelScope.launch {
            isAssistantTyping = true
            kotlinx.coroutines.delay(1500) // Simulate thinking

            val response = try {
                processAssistantInput(text, context)
            } catch (e: Exception) {
                context.getString(de.vvo.glassapp.R.string.assistant_error)
            }

            assistantMessages.add(ChatMessage(response, false))
            isAssistantTyping = false
        }
    }

    var assistantAction by mutableStateOf<String?>(null)

    private suspend fun processAssistantInput(input: String, context: android.content.Context): String {
        val lowInput = input.lowercase()
        return when {
            lowInput.contains("hallo") || lowInput.contains("hi") || lowInput.contains("hey") ->
                context.getString(de.vvo.glassapp.R.string.assistant_initial_response)

            lowInput.contains("karte") || lowInput.contains("map") || lowInput.contains("wo bin ich") -> {
                assistantAction = "navigate:map"
                "Natürlich! Ich öffne die Karte für dich."
            }

            lowInput.contains("abfahrt") || lowInput.contains("wann") || lowInput.contains("nächste") -> {
                val query = input.replace("abfahrt", "").replace("wann", "").replace("nächste", "").trim()
                val stops = if (query.length > 1) repository.searchStops(query) else emptyList()
                if (stops.isNotEmpty()) {
                    val targetStop = stops.first()
                    val departures = repository.getDepartures(targetStop.id)
                    if (departures.isNotEmpty()) {
                        val first = departures.first()
                        context.getString(de.vvo.glassapp.R.string.assistant_departure_found, targetStop.name, first.lineName, first.direction, first.scheduledTime)
                    } else {
                        context.getString(de.vvo.glassapp.R.string.assistant_no_departures, targetStop.name)
                    }
                } else {
                    context.getString(de.vvo.glassapp.R.string.assistant_stop_not_found)
                }
            }

            lowInput.contains("verspätung") || lowInput.contains("stau") || lowInput.contains("probleme") ->
                context.getString(de.vvo.glassapp.R.string.assistant_delay_info)

            lowInput.contains("favoriten") || lowInput.contains("stern") -> {
                "Deine Favoriten sind: " + _favorites.value.joinToString { it.name }
            }

            lowInput.contains("danke") || lowInput.contains("super") || lowInput.contains("cool") ->
                context.getString(de.vvo.glassapp.R.string.assistant_thanks_response)

            else -> context.getString(de.vvo.glassapp.R.string.assistant_fallback)
        }
    }

    fun searchStops(query: String) {
        viewModelScope.launch {
            try {
                _searchResults.value = repository.searchStops(query)
                _locationResults.value = repository.searchLocations(query)
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _searchResults.value = emptyList()
            }
        }
    }

    fun loadMapData(swLat: Double, swLon: Double, neLat: Double, neLon: Double) {
        viewModelScope.launch {
            try {
                val pins = repository.getMapPins(swLat, swLon, neLat, neLon)
                _mapPins.value = pins
                val stops = repository.getStopsInArea(swLat, swLon, neLat, neLon)
                _mapStops.value = stops
            } catch (e: Exception) {
                Log.e(TAG, "Map data load failed", e)
            }
        }
    }

    fun toggleFavorite(name: String, stopId: String) {
        val current = _favorites.value.toMutableList()
        val existing = current.find { it.stopId == stopId }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(Favorite(name, stopId))
        }
        _favorites.value = current
        favoritesManager.saveFavorites(current)
    }

    fun selectStop(stop: Stop) {
        _selectedStop.value = stop
        _selectedVehicle.value = null
        _selectedVehicleRoute.value = emptyList()
        viewModelScope.launch {
            try {
                if (stop.lat == null) {
                    val search = repository.searchStops(stop.id)
                    search.find { it.id == stop.id }?.let { _selectedStop.value = it }
                }
                _departures.value = repository.getDepartures(stop.id)
            } catch (e: Exception) {
                Log.e(TAG, "Departures load failed", e)
            }
        }
    }

    fun selectVehicle(vehicle: VehiclePin) {
        _selectedVehicle.value = vehicle
        _selectedStop.value = null
        _selectedVehicleRoute.value = emptyList()
        viewModelScope.launch {
            try {
                _selectedVehicleRoute.value = repository.getRoute(vehicle.id)
            } catch (e: Exception) {
                Log.e(TAG, "Route load failed", e)
            }
        }
    }

    fun deselectAll() {
        _selectedVehicle.value = null
        _selectedStop.value = null
        _selectedVehicleRoute.value = emptyList()
    }

    fun findTrips(originId: String?, destinationId: String) {
        val origin = originId ?: _userLocation.value?.let { "coord:${it.longitude}:${it.latitude}" } ?: "33000028"
        viewModelScope.launch {
            isSearchingTrips = true
            try {
                _trips.value = repository.getTrips(origin, destinationId)
            } catch (e: Exception) {
                Log.e(TAG, "Trip search failed", e)
                _trips.value = emptyList()
            } finally {
                isSearchingTrips = false
            }
        }
    }
}
