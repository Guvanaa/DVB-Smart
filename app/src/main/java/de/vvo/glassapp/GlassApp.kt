package de.vvo.glassapp

import android.app.Application
import de.vvo.glassapp.util.ServiceLocator
import com.mapbox.mapboxsdk.Mapbox

class GlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this)
        ServiceLocator.init(this)
    }
}
