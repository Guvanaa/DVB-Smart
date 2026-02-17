package de.vvo.glassapp.data.api

import de.vvo.glassapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface VvoApi {
    @GET("stopsearch/stop")
    suspend fun searchStop(
        @Query("query") query: String,
        @Query("stopsOnly") stopsOnly: Boolean = true
    ): StopSearchResponse

    @GET("dm")
    suspend fun getDepartures(
        @Query("stopid") stopId: String,
        @Query("limit") limit: Int = 20,
        @Query("mot") mot: String = "Tram,CityBus,SuburbanRailway"
    ): DepartureResponse

    @GET("map/pins")
    suspend fun getMapPins(
        @Query("swlat") swLat: Double,
        @Query("swlon") swLon: Double,
        @Query("nelat") neLat: Double,
        @Query("nelon") neLon: Double
    ): MapPinsResponse

    @GET("map/route")
    suspend fun getRoute(
        @Query("tripid") tripId: String
    ): RouteResponse

    @GET("tr/trips")
    suspend fun getTrips(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("isarrival") isArrival: Boolean = false,
        @Query("limit") limit: Int = 5
    ): TripResponse
}

interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("lat") lat: Double? = 51.0509, // Dresden
        @Query("lon") lon: Double? = 13.7373,
        @Query("lang") lang: String = "de"
    ): PhotonResponse
}
