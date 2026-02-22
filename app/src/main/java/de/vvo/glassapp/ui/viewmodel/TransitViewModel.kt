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

data class Favorite(
    val name: String,
    val stopId: String,
    val iconName: String = "Star" // Default icon
)

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

    private val _customOrigin = MutableStateFlow<Stop?>(null)
    val customOrigin: StateFlow<Stop?> = _customOrigin

    fun setCustomOrigin(stop: Stop?) {
        _customOrigin.value = stop
    }

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = LatLng(lat, lon)
    }

    fun refreshLocation(context: android.content.Context) {
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager

            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    updateUserLocation(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(p0: String?, p1: Int, p2: android.os.Bundle?) {}
                override fun onProviderEnabled(p0: String) {}
                override fun onProviderDisabled(p0: String) {}
            }

            locationManager.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER, 1000L, 10f, listener
            )
            locationManager.requestLocationUpdates(
                android.location.LocationManager.NETWORK_PROVIDER, 1000L, 10f, listener
            )

            val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            location?.let {
                updateUserLocation(it.latitude, it.longitude)
            } ?: run {
                if (_userLocation.value == null) {
                    updateUserLocation(51.0509, 13.7373)
                }
            }
        } catch (e: SecurityException) {
            if (_userLocation.value == null) {
                updateUserLocation(51.0509, 13.7373)
            }
        }
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
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when {
            lowInput.contains("hallo") || lowInput.contains("hi") || lowInput.contains("hey") || lowInput.contains("moin") || lowInput.contains("servus") -> {
                when {
                    hour in 5..11 -> context.getString(de.vvo.glassapp.R.string.assistant_greeting_morning)
                    hour in 18..23 -> context.getString(de.vvo.glassapp.R.string.assistant_greeting_evening)
                    else -> context.getString(de.vvo.glassapp.R.string.assistant_initial_response)
                }
            }

            lowInput.contains("karte") || lowInput.contains("map") || lowInput.contains("wo bin ich") || lowInput.contains("standort") -> {
                assistantAction = "navigate:map"
                "Gerne! Ich zeige dir deinen aktuellen Standort und den Live-Verkehr auf der Karte."
            }

            lowInput.contains("von") && lowInput.contains("nach") || lowInput.contains("zum") -> {
                val parts = if (lowInput.contains("nach")) lowInput.split("nach") else lowInput.split("zum")
                val originPart = parts[0].replace("von", "").trim()
                val destPart = parts[1].trim()

                val originStops = repository.searchStops(originPart)
                val destStops = repository.searchStops(destPart)

                if (originStops.isNotEmpty() && destStops.isNotEmpty()) {
                    findTrips(originStops.first().id, destStops.first().id)
                    "Alles klar! Ich suche nach einer Verbindung von ${originStops.first().name} nach ${destStops.first().name}."
                } else {
                    "Ich konnte eines der Ziele nicht finden. Meintest du vielleicht etwas anderes?"
                }
            }

            lowInput.contains("abfahrt") || lowInput.contains("wann") || lowInput.contains("nächste") || lowInput.contains("bahn") || lowInput.contains("bus") -> {
                val query = input.lowercase().replace("abfahrt", "").replace("wann", "").replace("nächste", "")
                    .replace("bahn", "").replace("bus", "").replace("straßenbahn", "").replace("für", "").trim()

                val stops = if (query.length > 2) repository.searchStops(query) else emptyList()
                if (stops.isNotEmpty()) {
                    val targetStop = stops.first()
                    val departures = repository.getDepartures(targetStop.id)
                    if (departures.isNotEmpty()) {
                        val first = departures.first()
                        context.getString(de.vvo.glassapp.R.string.assistant_departure_found, targetStop.name, first.lineName, first.direction, first.scheduledTime)
                    } else {
                        context.getString(de.vvo.glassapp.R.string.assistant_no_departures, targetStop.name)
                    }
                } else if (query.isEmpty()) {
                    "Für welche Haltestelle soll ich nachsehen? Sag mir einfach den Namen, zum Beispiel 'Postplatz'."
                } else {
                    context.getString(de.vvo.glassapp.R.string.assistant_stop_not_found)
                }
            }

            lowInput.contains("verspätung") || lowInput.contains("stau") || lowInput.contains("probleme") || lowInput.contains("störung") -> {
                val delayPhrases = listOf(
                    context.getString(de.vvo.glassapp.R.string.assistant_delay_info),
                    "Ich habe mal nachgesehen: Im Großen und Ganzen läuft alles nach Plan. Es gibt nur kleine Verzögerungen bei den Baustellen auf der Kesselsdorfer Straße.",
                    "Aktuell gibt es eine Störung auf der Linie 4 wegen eines Rettungseinsatzes. Alle anderen Linien fahren planmäßig.",
                    "Gute Nachrichten: Es sind aktuell keine Störungen im DVB-Netz bekannt. Deine Fahrt sollte also pünktlich sein!"
                )
                delayPhrases.random()
            }

            lowInput.contains("favoriten") || lowInput.contains("stern") || lowInput.contains("gespeichert") || lowInput.contains("merkliste") -> {
                if (_favorites.value.isEmpty()) {
                    "Du hast noch keine Favoriten. Suche eine Haltestelle und tippe auf den Stern, um sie hier zu speichern."
                } else {
                    "Deine aktuellen Favoriten sind: " + _favorites.value.joinToString { it.name } + ". Tippe auf einen auf der Startseite, um direkt eine Verbindung von deinem Standort zu planen."
                }
            }

            lowInput.contains("hilf") || lowInput.contains("hilfe") || lowInput.contains("was kannst du") || lowInput.contains("optionen") -> {
                val helpPhrases = listOf(
                    "Ich bin Lunina, deine DVB-Assistentin. Ich kann Abfahrten finden (z.B. 'Wann fährt die 3 am Hauptbahnhof?'), Verbindungen planen, dir die Karte zeigen oder dir Fakten über den DVB erzählen.",
                    "Du kannst mich alles über den DVB fragen! Zum Beispiel: 'Wie komme ich zum Albertplatz?' oder 'Gibt es heute Verspätungen?'",
                    "Ich helfe dir gerne! Ich kenne alle Haltestellen in Dresden und kann dir in Echtzeit sagen, wann dein nächster Bus kommt."
                )
                helpPhrases.random()
            }

            lowInput.contains("danke") || lowInput.contains("super") || lowInput.contains("cool") || lowInput.contains("toll") || lowInput.contains("vielen dank") ->
                context.getString(de.vvo.glassapp.R.string.assistant_thanks_response)

            lowInput.contains("wetter") || lowInput.contains("regen") || lowInput.contains("sonne") || lowInput.contains("kalt") || lowInput.contains("warm") ->
                context.getString(de.vvo.glassapp.R.string.assistant_weather)

            lowInput.contains("ticket") || lowInput.contains("fahrkarte") || lowInput.contains("preis") || lowInput.contains("kosten") || lowInput.contains("fairtiq") ->
                "Für Tickets empfehle ich dir die FAIRTIQ-App! Einfach beim Einsteigen wischen und immer den günstigsten Preis zahlen. Die App ist der perfekte Begleiter im VVO-Raum, da sie immer den günstigsten Tarif für dich berechnet. Probiere es mal aus!"

            lowInput.contains("tag") || lowInput.contains("morgen") || lowInput.contains("abend") || lowInput.contains("nacht") ->
                context.getString(de.vvo.glassapp.R.string.assistant_nice_day)

            lowInput.contains("wusstest") || lowInput.contains("fakten") || lowInput.contains("wissen") || lowInput.contains("erzähl") || lowInput.contains("info") -> {
                val facts = listOf(
                    context.getString(de.vvo.glassapp.R.string.assistant_did_you_know),
                    context.getString(de.vvo.glassapp.R.string.assistant_fun_fact_1),
                    context.getString(de.vvo.glassapp.R.string.assistant_fun_fact_2)
                )
                facts.random()
            }

            else -> context.getString(de.vvo.glassapp.R.string.assistant_fallback)
        }
    }

    fun searchStops(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            _locationResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val stops = repository.searchStops(query)
                _searchResults.value = stops
            } catch (e: Exception) {
                Log.e(TAG, "Stop search failed", e)
                _searchResults.value = emptyList()
            }
            try {
                val locations = repository.searchLocations(query)
                _locationResults.value = locations
            } catch (e: Exception) {
                Log.e(TAG, "Location search failed", e)
                _locationResults.value = emptyList()
            }
        }
    }

    fun loadMapData(swLat: Double, swLon: Double, neLat: Double, neLon: Double) {
        // Only load if area is reasonable (to avoid overwhelming API and UI)
        if (kotlin.math.abs(neLat - swLat) > 0.1 || kotlin.math.abs(neLon - swLon) > 0.15) {
            return
        }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading map data for: SW($swLat, $swLon) NE($neLat, $neLon)")
                val pins = repository.getMapPins(swLat, swLon, neLat, neLon)
                _mapPins.value = pins
                Log.d(TAG, "Fetched ${pins.size} pins")

                val stops = repository.getStopsInArea(swLat, swLon, neLat, neLon)
                _mapStops.value = stops
                Log.d(TAG, "Fetched ${stops.size} stops")
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
            current.add(Favorite(name, stopId, "Star"))
        }
        _favorites.value = current
        favoritesManager.saveFavorites(current)
    }

    fun updateFavoriteIcon(stopId: String, newIcon: String) {
        val current = _favorites.value.map {
            if (it.stopId == stopId) it.copy(iconName = newIcon) else it
        }
        _favorites.value = current
        favoritesManager.saveFavorites(current)
    }

    fun updateFavoriteName(stopId: String, newName: String) {
        val current = _favorites.value.map {
            if (it.stopId == stopId) it.copy(name = newName) else it
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
                if (stop.latitudeValue() == null) {
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

    fun findTrips(originId: String?, destinationId: String, time: String? = null, isArrival: Boolean = false) {
        val origin = originId
            ?: _customOrigin.value?.id
            ?: _userLocation.value?.let {
                val (right, up) = de.vvo.glassapp.util.CoordinateUtils.wgs84ToGk4(it.latitude, it.longitude)
                "coord:${right.toLong()}:${up.toLong()}"
            }
            ?: "33000028"

        viewModelScope.launch {
            isSearchingTrips = true
            try {
                _trips.value = repository.getTrips(origin, destinationId, isArrival, time)
            } catch (e: Exception) {
                Log.e(TAG, "Trip search failed", e)
                _trips.value = emptyList()
            } finally {
                isSearchingTrips = false
            }
        }
    }
}
