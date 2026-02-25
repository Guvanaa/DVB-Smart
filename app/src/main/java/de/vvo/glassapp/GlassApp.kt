package de.vvo.glassapp

import android.app.Application
import de.vvo.glassapp.util.ServiceLocator
import org.maplibre.android.MapLibre

class GlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        ServiceLocator.init(this)
    }
}
