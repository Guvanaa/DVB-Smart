package de.vvo.glassapp.data.model

import com.google.gson.annotations.SerializedName

data class Stop(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Place") val place: String? = null,
    @SerializedName("Lat") val lat: Double? = null,
    @SerializedName("Lon") val lon: Double? = null
)

data class StopSearchResponse(
    @SerializedName("Stops") val stops: List<Stop>
)

data class Departure(
    @SerializedName("Id") val id: String,
    @SerializedName("LineName") val lineName: String,
    @SerializedName("Direction") val direction: String,
    @SerializedName("Platform") val platform: Platform? = null,
    @SerializedName("RealTime") val realTime: String? = null,
    @SerializedName("ScheduledTime") val scheduledTime: String,
    @SerializedName("State") val state: String? = null,
    @SerializedName("Type") val type: String? = null
)

data class Platform(
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String
)

data class DepartureResponse(
    @SerializedName("Departures") val departures: List<Departure>,
    @SerializedName("Name") val name: String
)

data class VehiclePin(
    @SerializedName("Id") val id: String,
    @SerializedName("Lat") val lat: Double,
    @SerializedName("Lon") val lon: Double,
    @SerializedName("Line") val line: String,
    @SerializedName("Dir") val direction: String,
    @SerializedName("Type") val type: String,
    @SerializedName("Punctuality") val punctuality: Int? = null // Delay in minutes
)

data class MapPinsResponse(
    @SerializedName("Pins") val pins: List<VehiclePin>
)

data class StopPoint(
    @SerializedName("Name") val name: String,
    @SerializedName("Lat") val lat: Double,
    @SerializedName("Lon") val lon: Double,
    @SerializedName("Time") val time: String? = null,
    @SerializedName("Delay") val delay: Int? = null
)

data class RouteResponse(
    @SerializedName("Stops") val stops: List<StopPoint>
)

data class TripResponse(
    @SerializedName("Trips") val trips: List<Trip>
)

data class Trip(
    @SerializedName("Duration") val duration: Int,
    @SerializedName("Interchanges") val interchanges: Int,
    @SerializedName("DepartureTime") val departureTime: String,
    @SerializedName("ArrivalTime") val arrivalTime: String,
    @SerializedName("Sections") val sections: List<Section>
)

data class Section(
    @SerializedName("Type") val type: String,
    @SerializedName("Line") val line: String? = null,
    @SerializedName("Direction") val direction: String? = null
)

data class ChatMessage(val text: String, val isFromUser: Boolean)
