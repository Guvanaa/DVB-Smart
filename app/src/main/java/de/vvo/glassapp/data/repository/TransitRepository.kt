package de.vvo.glassapp.data.repository

import de.vvo.glassapp.data.api.PhotonApi
import de.vvo.glassapp.data.api.VvoApi
import de.vvo.glassapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransitRepository(
    private val vvoApi: VvoApi,
    private val photonApi: PhotonApi
) {
    suspend fun searchStops(query: String) = withContext(Dispatchers.IO) {
        vvoApi.searchStop(query).stops
    }

    suspend fun getDepartures(stopId: String) = withContext(Dispatchers.IO) {
        vvoApi.getDepartures(stopId).departures
    }

    suspend fun getMapPins(swLat: Double, swLon: Double, neLat: Double, neLon: Double) = withContext(Dispatchers.IO) {
        vvoApi.getMapPins(swLat, swLon, neLat, neLon).pins
    }

    suspend fun getRoute(tripId: String) = withContext(Dispatchers.IO) {
        vvoApi.getRoute(tripId).stops
    }

    suspend fun getTrips(origin: String, destination: String) = withContext(Dispatchers.IO) {
        vvoApi.getTrips(origin, destination).trips
    }

    suspend fun searchLocations(query: String) = withContext(Dispatchers.IO) {
        photonApi.search(query).features
    }
}
