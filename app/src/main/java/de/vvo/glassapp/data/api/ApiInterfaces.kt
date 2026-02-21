package de.vvo.glassapp.data.api

import de.vvo.glassapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface VvoApi {
    @GET("st/find")
    suspend fun searchStop(
        @Query("query") query: String,
        @Query("stopsOnly") stopsOnly: Boolean = true,
        @Query("dvbOnly") dvbOnly: Boolean = false,
        @Query("format") format: String = "json"
    ): StopSearchResponse

    @GET("dm")
    suspend fun getDepartures(
        @Query("stopid") stopId: String,
        @Query("limit") limit: Int = 20,
        @Query("mot") mot: String = "Tram,CityBus,SuburbanRailway,Train,Cableway,Ferry",
        @Query("format") format: String = "json"
    ): DepartureResponse

    @GET("map/pins")
    suspend fun getMapPins(
        @Query("swlat") swLat: Double,
        @Query("swlon") swLon: Double,
        @Query("nelat") neLat: Double,
        @Query("nelon") neLon: Double,
        @Query("showtrips") showTrips: Boolean = true,
        @Query("format") format: String = "json"
    ): MapPinsResponse

    @GET("map/stops")
    suspend fun getStopsInArea(
        @Query("swlat") swLat: Double,
        @Query("swlon") swLon: Double,
        @Query("nelat") neLat: Double,
        @Query("nelon") neLon: Double
    ): MapStopsResponse

    @GET("map/route")
    suspend fun getRoute(
        @Query("tripid") tripId: String
    ): RouteResponse

    @GET("tr/trips")
    suspend fun getTrips(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("isarrival") isArrival: Boolean = false,
        @Query("time") time: String? = null,
        @Query("limit") limit: Int = 5,
        @Query("mot") mot: String = "Tram,CityBus,SuburbanRailway,Train,Cableway,Ferry,Footway",
        @Query("format") format: String = "json"
    ): TripResponse
}

interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("lat") lat: Double? = 51.0509,
        @Query("lon") lon: Double? = 13.7373,
        @Query("lang") lang: String = "de",
        @Query("bbox") bbox: String = "13.5,50.9,14.0,51.2" // Bound to Dresden area
    ): PhotonResponse
}
