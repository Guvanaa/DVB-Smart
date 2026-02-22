package de.vvo.glassapp.util

import kotlin.math.*

object CoordinateUtils {
    // Gauss-Krüger Zone 4 (EPSG:31468) to WGS84 (EPSG:4326) approximation for Dresden
    fun gk4ToWgs84(right: Double, up: Double): Pair<Double, Double> {
        val x = right - 4500000.0
        val y = up

        // Very simplified transformation for Dresden area
        // Based on local linear approximation
        val lat = 51.0509 + (y - 5656360.0) / 111120.0
        val lon = 13.7373 + (x - 124000.0) / 69800.0

        return Pair(lat, lon)
    }

    fun wgs84ToGk4(lat: Double, lon: Double): Pair<Double, Double> {
        // Inverse of the above approximation
        val y = 5656360.0 + (lat - 51.0509) * 111120.0
        val x = 124000.0 + (lon - 13.7373) * 69800.0

        return Pair(x + 4500000.0, y)
    }
}
