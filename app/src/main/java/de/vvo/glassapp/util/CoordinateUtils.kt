package de.vvo.glassapp.util

import kotlin.math.*

/**
 * Coordinate transformations for Dresden (VVO Area).
 * The VVO API uses Gauss-Krüger Zone 4 (GK4) for many legacy endpoints.
 * This class provides a high-accuracy local linear approximation for the Dresden area.
 */
object CoordinateUtils {

    // Reference Point: Dresden Hauptbahnhof
    private const val REF_LAT = 51.0405
    private const val REF_LON = 13.7315
    private const val REF_UP = 5654921.0
    private const val REF_RIGHT = 4621213.0

    // High-precision scaling factors derived from multiple points in Dresden
    private const val LAT_PER_UP = 0.0000089982
    private const val LON_PER_RIGHT = 0.0000153915

    /**
     * Converts GK4 coordinates to WGS84 (GPS).
     * @param right Rechtswert (usually starts with 4... in Dresden)
     * @param up Hochwert (usually starts with 5... in Dresden)
     */
    fun gk4ToWgs84(right: Double, up: Double): Pair<Double, Double> {
        if (right == 0.0 || up == 0.0) return Pair(0.0, 0.0)

        // Handle coordinates with and without zone prefix (4)
        val normalizedRight = if (right < 1000000) right + 4000000 else right

        val lat = REF_LAT + (up - REF_UP) * LAT_PER_UP
        val lon = REF_LON + (normalizedRight - REF_RIGHT) * LON_PER_RIGHT

        return Pair(lat, lon)
    }

    /**
     * Converts WGS84 (GPS) to GK4.
     */
    fun wgs84ToGk4(lat: Double, lon: Double): Pair<Double, Double> {
        val up = REF_UP + (lat - REF_LAT) / LAT_PER_UP
        val right = REF_RIGHT + (lon - REF_LON) / LON_PER_RIGHT

        return Pair(right, up)
    }
}
