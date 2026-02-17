package de.vvo.glassapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.vvo.glassapp.data.model.Departure
import de.vvo.glassapp.data.model.Stop
import de.vvo.glassapp.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TransitViewModel(private val repository: TransitRepository) : ViewModel() {
    private val _searchResults = MutableStateFlow<List<Stop>>(emptyList())
    val searchResults: StateFlow<List<Stop>> = _searchResults

    private val _locationResults = MutableStateFlow<List<de.vvo.glassapp.data.model.PhotonFeature>>(emptyList())
    val locationResults: StateFlow<List<de.vvo.glassapp.data.model.PhotonFeature>> = _locationResults

    private val _departures = MutableStateFlow<List<Departure>>(emptyList())
    val departures: StateFlow<List<Departure>> = _departures

    private val _mapPins = MutableStateFlow<List<VehiclePin>>(emptyList())
    val mapPins: StateFlow<List<VehiclePin>> = _mapPins

    private val _selectedVehicleRoute = MutableStateFlow<List<StopPoint>>(emptyList())
    val selectedVehicleRoute: StateFlow<List<StopPoint>> = _selectedVehicleRoute

    private val _selectedVehicle = MutableStateFlow<VehiclePin?>(null)
    val selectedVehicle: StateFlow<VehiclePin?> = _selectedVehicle

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    fun searchStops(query: String) {
        viewModelScope.launch {
            try {
                _searchResults.value = repository.searchStops(query)
                // Also search for locations to satisfy POI/Address requirement
                _locationResults.value = repository.searchLocations(query)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadDepartures(stopId: String) {
        viewModelScope.launch {
            try {
                _departures.value = repository.getDepartures(stopId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadMapPins(swLat: Double, swLon: Double, neLat: Double, neLon: Double) {
        viewModelScope.launch {
            try {
                _mapPins.value = repository.getMapPins(swLat, swLon, neLat, neLon)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectVehicle(vehicle: VehiclePin) {
        _selectedVehicle.value = vehicle
        viewModelScope.launch {
            try {
                _selectedVehicleRoute.value = repository.getRoute(vehicle.id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deselectVehicle() {
        _selectedVehicle.value = null
        _selectedVehicleRoute.value = emptyList()
    }

    fun findTrips(originId: String, destinationId: String) {
        viewModelScope.launch {
            try {
                _trips.value = repository.getTrips(originId, destinationId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
