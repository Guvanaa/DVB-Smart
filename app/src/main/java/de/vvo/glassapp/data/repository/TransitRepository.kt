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
        vvoApi.searchStop(query).stops ?: emptyList()
    }

    suspend fun getDepartures(stopId: String) = withContext(Dispatchers.IO) {
        vvoApi.getDepartures(stopId).departures ?: emptyList()
    }

    suspend fun getMapPins(swLat: Double, swLon: Double, neLat: Double, neLon: Double) = withContext(Dispatchers.IO) {
        vvoApi.getMapPins(swLat, swLon, neLat, neLon).pins ?: emptyList()
    }

    suspend fun getStopsInArea(swLat: Double, swLon: Double, neLat: Double, neLon: Double) = withContext(Dispatchers.IO) {
        vvoApi.getStopsInArea(swLat, swLon, neLat, neLon).stops ?: emptyList()
    }

    suspend fun getRoute(tripId: String) = withContext(Dispatchers.IO) {
        vvoApi.getRoute(tripId).stops ?: emptyList()
    }

    suspend fun getTrips(origin: String, destination: String) = withContext(Dispatchers.IO) {
        vvoApi.getTrips(origin, destination).trips ?: emptyList()
    }

    suspend fun searchLocations(query: String) = withContext(Dispatchers.IO) {
        photonApi.search(query).features ?: emptyList()
    }
}
