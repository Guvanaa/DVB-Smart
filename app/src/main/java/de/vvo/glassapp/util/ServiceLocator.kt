package de.vvo.glassapp.util

import de.vvo.glassapp.data.api.PhotonApi
import de.vvo.glassapp.data.api.VvoApi
import de.vvo.glassapp.data.repository.TransitRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceLocator {
    private val vvoRetrofit = Retrofit.Builder()
        .baseUrl("https://webapi.vvo-online.de/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val photonRetrofit = Retrofit.Builder()
        .baseUrl("https://photon.komoot.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val vvoApi = vvoRetrofit.create(VvoApi::class.java)
    private val photonApi = photonRetrofit.create(PhotonApi::class.java)

    val repository = TransitRepository(vvoApi, photonApi)
}
