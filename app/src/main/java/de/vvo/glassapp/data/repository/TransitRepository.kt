package de.vvo.glassapp.data.repository

import de.vvo.glassapp.data.api.PhotonApi
import de.vvo.glassapp.data.api.VvoApi
import de.vvo.glassapp.data.model.*
import de.vvo.glassapp.util.CoordinateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransitRepository(
    private val vvoApi: VvoApi,
    private val photonApi: PhotonApi
) {
    suspend fun searchStops(query: String): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val response = vvoApi.searchStop(query)
            if (response.status?.code == "Ok" && !response.stops.isNullOrEmpty()) {
                return@withContext response.stops
            }

            // Fallback to PointFinder
            val pfResponse = vvoApi.pointFinder(query)
            pfResponse.points?.mapNotNull { pointStr ->
                val parts = pointStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val name = parts[3]
                    val place = parts[2]
                    val up = parts[4].toDoubleOrNull() ?: 0.0
                    val right = parts[5].toDoubleOrNull() ?: 0.0
                    val (lat, lon) = CoordinateUtils.gk4ToWgs84(right, up)
                    Stop(id, name, place, lat, lon)
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Search failed", e)
            emptyList()
        }
    }

    suspend fun getDepartures(stopId: String): List<Departure> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf(
                "stopid" to stopId,
                "limit" to 20,
                "format" to "json"
            )
            val response = vvoApi.getDepartures(body)
            response.departures ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Departures failed", e)
            emptyList()
        }
    }

    suspend fun getStopsInArea(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val (swR, swU) = CoordinateUtils.wgs84ToGk4(swLat, swLon)
            val (neR, neU) = CoordinateUtils.wgs84ToGk4(neLat, neLon)

            val body = mapOf(
                "swlat" to swU.toLong().toString(),
                "swlng" to swR.toLong().toString(),
                "nelat" to neU.toLong().toString(),
                "nelng" to neR.toLong().toString(),
                "pintypes" to "Stop",
                "format" to "json"
            )
            val response = vvoApi.getMapPins(body)
            response.pins?.mapNotNull { pinStr ->
                val parts = pinStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val name = parts[3]
                    val place = parts[2]
                    val up = parts[4].toDoubleOrNull() ?: 0.0
                    val right = parts[5].toDoubleOrNull() ?: 0.0
                    val (lat, lon) = CoordinateUtils.gk4ToWgs84(right, up)
                    Stop(id, name, place, lat, lon)
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMapPins(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<VehiclePin> = withContext(Dispatchers.IO) {
        try {
            val (swR, swU) = CoordinateUtils.wgs84ToGk4(swLat, swLon)
            val (neR, neU) = CoordinateUtils.wgs84ToGk4(neLat, neLon)

            val body = mapOf(
                "swlat" to swU.toLong().toString(),
                "swlng" to swR.toLong().toString(),
                "nelat" to neU.toLong().toString(),
                "nelng" to neR.toLong().toString(),
                "pintypes" to "Stop,Vehicle",
                "showtrips" to true,
                "format" to "json"
            )
            val response = vvoApi.getMapPins(body)
            response.pins?.mapNotNull { pinStr ->
                val parts = pinStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val up = parts[4].toDoubleOrNull() ?: 0.0
                    val right = parts[5].toDoubleOrNull() ?: 0.0
                    val (lat, lon) = CoordinateUtils.gk4ToWgs84(right, up)

                    if (id.startsWith("tr:")) {
                        // Trip/Vehicle
                        val line = parts[3]
                        val direction = parts[2]
                        val delay = if (parts.size > 6) parts[6].toIntOrNull() else 0
                        VehiclePin(id, lat, lon, line, direction, "Vehicle", punctuality = delay)
                    } else if (id.length >= 8) {
                        // Likely a stop
                        null // We handle stops in getStopsInArea
                    } else null
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Map pins failed", e)
            emptyList()
        }
    }

    suspend fun getTrips(origin: String, destination: String, isArrival: Boolean = false, time: String? = null): List<Trip> = withContext(Dispatchers.IO) {
        try {
            val body = mutableMapOf<String, Any>(
                "origin" to origin,
                "destination" to destination,
                "format" to "json",
                "standardSettings" to mapOf(
                    "mot" to listOf("Tram", "CityBus", "SuburbanRailway", "Train", "Cableway", "Ferry", "Footway")
                )
            )
            if (time != null) {
                body["time"] = time
                body["isarrivaltime"] = isArrival
            }

            val response = vvoApi.getTrips(body)
            response.routes?.map { route ->
                Trip(
                    duration = route.duration,
                    interchanges = route.interchanges,
                    departureTime = route.departureTime ?: "",
                    arrivalTime = route.arrivalTime ?: "",
                    sections = route.motChain?.map { item ->
                        Section(
                            type = item.type ?: "Walking",
                            line = item.name,
                            direction = item.direction
                        )
                    } ?: emptyList()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Trips failed", e)
            emptyList()
        }
    }

    suspend fun getRoute(tripId: String): List<StopPoint> = withContext(Dispatchers.IO) {
        try {
            // tripId here is usually "tr:XXXXX"
            val body = mapOf(
                "tripid" to tripId,
                "format" to "json"
            )
            val response = vvoApi.getTripDetails(body)
            response.stops ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Route failed", e)
            emptyList()
        }
    }

    suspend fun searchLocations(query: String) = withContext(Dispatchers.IO) {
        try {
            photonApi.search(query).features ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
