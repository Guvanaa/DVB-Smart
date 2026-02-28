package de.vvo.glassapp.data.repository

import de.vvo.glassapp.data.api.OverpassApi
import de.vvo.glassapp.data.api.PhotonApi
import de.vvo.glassapp.data.api.VvoApi
import de.vvo.glassapp.data.model.*
import de.vvo.glassapp.util.CoordinateUtils
import de.vvo.glassapp.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransitRepository(
    private val vvoApi: VvoApi,
    private val photonApi: PhotonApi,
    private val overpassApi: OverpassApi
) {
    suspend fun searchStops(query: String): List<Stop> = withContext(Dispatchers.IO) {
        // Try /st/find first (may return 404 if deprecated)
        try {
            val response = vvoApi.searchStop(query)
            if (response.status?.code == "Ok" && !response.stops.isNullOrEmpty()) {
                return@withContext response.stops
            }
        } catch (_: Exception) {
            // /st/find unavailable – fall through to PointFinder
        }

        // Fallback: PointFinder (more reliable, returns IFOPT IDs)
        try {
            val pfResponse = vvoApi.pointFinder(query)
            pfResponse.points?.mapNotNull { pointStr ->
                val parts = pointStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val name = parts[3]
                    val place = parts[2]
                    val right = parts[5].toDoubleOrNull() ?: 0.0
                    val up = parts[4].toDoubleOrNull() ?: 0.0
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
            response.departures?.map { dep ->
                val delay = DateTimeUtils.calculateDelay(dep.scheduledTime, dep.realTime)
                dep.copy(
                    scheduledTime = DateTimeUtils.formatTime(dep.scheduledTime),
                    realTime = dep.realTime?.let { DateTimeUtils.formatTime(it) },
                    delay = delay
                )
            } ?: emptyList()
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
                "pintypes" to listOf("Stop"),
                "format" to "json"
            )
            val response = vvoApi.getMapPins(body)
            android.util.Log.d("StopsInArea", "Raw pins count: ${response.pins?.size}")
            response.pins?.take(3)?.forEach { android.util.Log.d("StopsInArea", "RAW PIN: $it") }
            response.pins?.mapNotNull { pinStr ->
                val parts = pinStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val name = parts[3]
                    val right = parts[5].toDoubleOrNull() ?: 0.0 // easting (Rechtswert, ~4.6M)
                    val up = parts[4].toDoubleOrNull() ?: 0.0     // northing (Hochwert, ~5.65M)
                    val (lat, lon) = CoordinateUtils.gk4ToWgs84(right, up)
                    android.util.Log.d("StopsInArea", "Stop '$name': parts[4]=${parts[4]}, parts[5]=${parts[5]} → lat=$lat, lon=$lon")
                    Stop(id, name, null, lat, lon)
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("StopsInArea", "Failed", e)
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
                "pintypes" to listOf("Vehicle"),
                "showtrips" to true,
                "format" to "json"
            )
            val response = vvoApi.getMapPins(body)
            response.pins?.mapNotNull { pinStr ->
                val parts = pinStr.split("|")
                if (parts.size >= 6) {
                    val id = parts[0]
                    val right = parts[4].toDoubleOrNull() ?: 0.0
                    val up = parts[5].toDoubleOrNull() ?: 0.0
                    val (lat, lon) = CoordinateUtils.gk4ToWgs84(right, up)

                    if (id.startsWith("tr:") || parts.size >= 6) {
                        // Trip/Vehicle
                        // Field order for vehicles can vary, but usually:
                        // id | type | direction | line | X | Y
                        // OR
                        // id | line | direction | type | X | Y

                        val isIdTr = id.startsWith("tr:")
                        val line = if (isIdTr) parts[3] else parts[1]
                        val direction = if (isIdTr) parts[2] else parts[2]

                        val delay = if (parts.size > 6) parts[6].toIntOrNull() else null
                        VehiclePin(id, lat, lon, line, direction, "Vehicle", punctuality = delay)
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
                    "mot" to listOf("Tram", "CityBus", "SuburbanRailway", "Train", "Cableway", "Ferry")
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
                    departureTime = DateTimeUtils.formatTime(route.departureTime),
                    arrivalTime = DateTimeUtils.formatTime(route.arrivalTime),
                    sections = route.motChain?.map { item ->
                        Section(
                            type = item.type ?: "Walking",
                            line = item.name,
                            direction = item.direction
                        )
                    } ?: emptyList(),
                    mapData = route.mapData
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

    /**
     * Fetches transit stop positions from OpenStreetMap via Overpass API.
     * Returns stops directly in WGS84 – no coordinate conversion needed.
     * Stops with ref:IFOPT tags use VVO-compatible IDs for departure queries.
     */
    suspend fun getOverpassStops(swLat: Double, swLon: Double, neLat: Double, neLon: Double): List<Stop> = withContext(Dispatchers.IO) {
        try {
            val query = """
                [out:json][timeout:25][bbox:$swLat,$swLon,$neLat,$neLon];
                (
                  node["public_transport"="stop_position"];
                  node["highway"="bus_stop"];
                  node["railway"="tram_stop"];
                );
                out body;
            """.trimIndent()
            val response = overpassApi.query(query)
            val seen = mutableSetOf<String>()
            response.elements
                ?.filter { it.type == "node" && it.lat != null && it.lon != null && it.tags?.get("name") != null }
                ?.mapNotNull { node ->
                    val name = node.tags!!["name"]!!
                    // Prefer ref:IFOPT as id (VVO-compatible); fall back to OSM node id
                    val id = node.tags["ref:IFOPT"] ?: "osm:${node.id}"
                    // Deduplicate: skip nodes with the same IFOPT (different platforms, same stop)
                    if (!seen.add(id)) return@mapNotNull null
                    Stop(
                        id = id,
                        name = name,
                        place = node.tags["addr:suburb"] ?: node.tags["addr:city"],
                        lat = node.lat,
                        lon = node.lon
                    )
                }
                ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransitRepository", "Overpass failed", e)
            emptyList()
        }
    }
}
