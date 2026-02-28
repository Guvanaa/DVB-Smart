package de.vvo.glassapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.vvo.glassapp.ui.screens.AssistantScreen
import de.vvo.glassapp.ui.screens.GlassProtoScreen
import de.vvo.glassapp.ui.screens.HomeScreen
import de.vvo.glassapp.ui.screens.MapScreen
import de.vvo.glassapp.ui.theme.*

import de.vvo.glassapp.util.ServiceLocator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "proto") {
        composable("home") { HomeScreen(navController) }
        composable("proto") { GlassProtoScreen(navController) }
        composable("assistant") { AssistantScreen(navController) }
        composable("map") { MapScreen(navController) }
        composable("map/{stopId}") { backStackEntry ->
            val stopId = backStackEntry.arguments?.getString("stopId")
            MapScreen(navController, stopId)
        }
        composable("map/{lat}/{lon}") { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            MapScreen(navController, lat = lat, lon = lon)
        }
    }
}
