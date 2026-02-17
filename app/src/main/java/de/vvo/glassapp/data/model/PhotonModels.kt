package de.vvo.glassapp.data.model

import com.google.gson.annotations.SerializedName

data class PhotonResponse(
    val features: List<PhotonFeature>
)

data class PhotonFeature(
    val geometry: PhotonGeometry,
    val properties: PhotonProperties
)

data class PhotonGeometry(
    val coordinates: List<Double> // [lon, lat]
)

data class PhotonProperties(
    val name: String,
    val city: String?,
    val street: String?,
    val housenumber: String?,
    val osm_value: String?
)
