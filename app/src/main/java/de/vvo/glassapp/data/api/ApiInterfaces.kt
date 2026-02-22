package de.vvo.glassapp.data.api

import de.vvo.glassapp.data.model.*
import retrofit2.http.*

interface VvoApi {
    @GET("st/find")
    suspend fun searchStop(
        @Query("query") query: String,
        @Query("stopsOnly") stopsOnly: Boolean = true,
        @Query("format") format: String = "json"
    ): StopSearchResponse

    @GET("tr/pointfinder")
    suspend fun pointFinder(
        @Query("query") query: String,
        @Query("stopsOnly") stopsOnly: Boolean = true,
        @Query("format") format: String = "json"
    ): PointFinderResponse

    @POST("dm")
    suspend fun getDepartures(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): DepartureResponse

    @POST("map/pins")
    suspend fun getMapPins(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): MapPinsResponse

    @POST("tr/trips")
    suspend fun getTrips(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): TripResponse

    @POST("dm/trip")
    suspend fun getTripDetails(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): RouteResponse
}

interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("lat") lat: Double? = 51.0509,
        @Query("lon") lon: Double? = 13.7373,
        @Query("lang") lang: String = "de",
        @Query("bbox") bbox: String = "13.5,50.9,14.0,51.2"
    ): PhotonResponse
}
