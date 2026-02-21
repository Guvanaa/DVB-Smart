package de.vvo.glassapp

import android.app.Application
import de.vvo.glassapp.util.ServiceLocator

class GlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
