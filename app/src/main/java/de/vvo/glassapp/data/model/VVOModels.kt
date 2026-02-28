package de.vvo.glassapp.data.model

import com.google.gson.annotations.SerializedName

data class Status(
    @SerializedName("Code") val code: String
)

data class Stop(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Place") val place: String? = null,
    @SerializedName("Lat") val lat: Double? = null,
    @SerializedName("Lon") val lon: Double? = null,
    @SerializedName("Latitude") val latitude: Double? = null,
    @SerializedName("Longitude") val longitude: Double? = null
) {
    fun latitudeValue(): Double? = lat ?: latitude
    fun longitudeValue(): Double? = lon ?: longitude
}

data class StopSearchResponse(
    @SerializedName("Stops") val stops: List<Stop>?,
    @SerializedName("Status") val status: Status?
)

data class PointFinderResponse(
    @SerializedName("Points") val points: List<String>?,
    @SerializedName("Status") val status: Status?
)

data class Departure(
    @SerializedName("Id") val id: String,
    @SerializedName("LineName") val lineName: String,
    @SerializedName("Direction") val direction: String,
    @SerializedName("Platform") val platform: Platform? = null,
    @SerializedName("RealTime") val realTime: String? = null,
    @SerializedName("ScheduledTime") val scheduledTime: String,
    @SerializedName("State") val state: String? = null,
    @SerializedName("Mot") val mot: String? = null,
    val delay: Int? = null
)

data class Platform(
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String
)

data class DepartureResponse(
    @SerializedName("Departures") val departures: List<Departure>?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Status") val status: Status?
)

data class VehiclePin(
    val id: String,
    val lat: Double,
    val lon: Double,
    val line: String,
    val direction: String,
    val type: String,
    val punctuality: Int? = null
) {
    fun latitudeValue(): Double = lat
    fun longitudeValue(): Double = lon
}

data class MapPinsResponse(
    @SerializedName("Pins") val pins: List<String>?,
    @SerializedName("Status") val status: Status?
)

data class MapStopsResponse(
    @SerializedName("Stops") val stops: List<Stop>?,
    @SerializedName("Status") val status: Status?
)

data class StopPoint(
    @SerializedName("Name") val name: String,
    @SerializedName("Place") val place: String? = null,
    @SerializedName("Lat") val lat: Double? = null,
    @SerializedName("Lon") val lon: Double? = null,
    @SerializedName("Time") val time: String? = null,
    @SerializedName("RealTime") val realTime: String? = null,
    @SerializedName("Delay") val delay: Int? = null
) {
    fun latitudeValue(): Double = lat ?: 0.0
    fun longitudeValue(): Double = lon ?: 0.0
}

data class RouteResponse(
    @SerializedName("Stops") val stops: List<StopPoint>?,
    @SerializedName("Status") val status: Status?
)

data class TripResponse(
    @SerializedName("Routes") val routes: List<Route>?,
    @SerializedName("Status") val status: Status?
)

data class Route(
    @SerializedName("Duration") val duration: Int,
    @SerializedName("Interchanges") val interchanges: Int,
    @SerializedName("DepartureTime") val departureTime: String?,
    @SerializedName("ArrivalTime") val arrivalTime: String?,
    @SerializedName("MotChain") val motChain: List<MotChainItem>?,
    @SerializedName("MapData") val mapData: List<String>?
)

data class MotChainItem(
    @SerializedName("Name") val name: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("Direction") val direction: String?
)

// Legacy compatibility for TripItem
data class Trip(
    val duration: Int,
    val interchanges: Int,
    val departureTime: String,
    val arrivalTime: String,
    val sections: List<Section>,
    val mapData: List<String>? = null
)

data class Section(
    val type: String,
    val line: String? = null,
    val direction: String? = null,
    val duration: Int? = null
)

data class ChatMessage(val text: String, val isFromUser: Boolean)

// ── Overpass API (OpenStreetMap) ─────────────────────────────────────────────

data class OverpassNode(
    @SerializedName("type") val type: String? = null,
    @SerializedName("id")   val id: Long = 0L,
    @SerializedName("lat")  val lat: Double? = null,
    @SerializedName("lon")  val lon: Double? = null,
    @SerializedName("tags") val tags: Map<String, String>? = null
)

data class OverpassResponse(
    @SerializedName("elements") val elements: List<OverpassNode>? = null
)
