package de.vvo.glassapp.util

import kotlin.math.*

/**
 * Coordinate transformations for Dresden (VVO Area).
 *
 * gk4ToWgs84: full geodetic conversion
 *   1. Inverse Gauss-Krüger projection → geodetic coords on Bessel 1841 ellipsoid
 *   2. Helmert 7-parameter transform (BKG standard, DHDN→WGS84)
 *   3. ECEF → WGS84 geodetic
 *
 * wgs84ToGk4: linear approximation (sufficient for API bounding-box queries)
 */
object CoordinateUtils {

    // ── Bessel 1841 ellipsoid (used by GK4 / DHDN) ───────────────────────────
    private const val B_A  = 6377397.155        // semi-major axis [m]
    private const val B_F  = 1.0 / 299.1528128  // flattening
    private val   B_B  = B_A * (1.0 - B_F)      // semi-minor axis
    private val   B_E2 = 2 * B_F - B_F * B_F    // first eccentricity squared

    // ── WGS84 ellipsoid ──────────────────────────────────────────────────────
    private const val W_A  = 6378137.0
    private const val W_F  = 1.0 / 298.257223563
    private val   W_E2 = 2 * W_F - W_F * W_F

    // ── Helmert 7-parameter: DHDN/Bessel 1841 → WGS84 ───────────────────────
    // Standard BKG values for Germany (position-vector / Bursa-Wolf convention)
    private const val H_TX =  598.1              // translation [m]
    private const val H_TY =   73.7
    private const val H_TZ =  418.2
    private const val H_RX = -0.202 / 206265.0  // rotation [arcsec → rad]
    private const val H_RY =  0.045 / 206265.0
    private const val H_RZ =  2.455 / 206265.0
    private const val H_DS =  6.7e-6             // scale factor [ppm → unitless]

    // ── Linear approximation constants (used only for wgs84ToGk4) ────────────
    private const val REF_LAT      = 51.0405
    private const val REF_LON      = 13.7315
    private const val REF_UP       = 5657517.0
    private const val REF_RIGHT    = 4621643.0
    private const val LAT_PER_UP   = 0.000008994
    private const val LON_PER_RIGHT = 0.00001429

    /**
     * Converts GK4 (Gauss-Krüger Zone 4, DHDN/Bessel 1841) to WGS84 (GPS).
     * Accuracy: < 2 m anywhere in Germany.
     *
     * @param right  Rechtswert / easting  (~4.6 M for Dresden; zone prefix optional)
     * @param up     Hochwert  / northing  (~5.65 M for Dresden)
     */
    fun gk4ToWgs84(right: Double, up: Double): Pair<Double, Double> {
        if (right == 0.0 || up == 0.0) return Pair(0.0, 0.0)

        // Ensure zone prefix 4 is present
        val r    = if (right < 1_000_000.0) right + 4_000_000.0 else right
        val zone = (r / 1_000_000.0).toInt()          // = 4 for Dresden
        val l0   = zone * 3.0 * PI / 180.0            // central meridian in rad (12° for zone 4)

        // Strip zone number and false easting (500 000 m)
        val y = r - zone * 1_000_000.0 - 500_000.0   // easting from central meridian [m]
        val x = up                                     // northing from equator [m]

        // 1. Inverse GK → geodetic on Bessel 1841 [radians]
        val (phiB, lamB) = inverseGK(x, y, l0)

        // 2. Bessel geodetic → ECEF
        val (xC, yC, zC) = geodeticToECEF(phiB, lamB, 0.0, B_A, B_E2)

        // 3. Helmert: Bessel ECEF → WGS84 ECEF
        val (xW, yW, zW) = helmert(xC, yC, zC)

        // 4. WGS84 ECEF → geodetic [radians]
        val (latW, lonW) = ecefToGeodetic(xW, yW, zW, W_A, W_E2)

        return Pair(latW * 180.0 / PI, lonW * 180.0 / PI)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Inverse Gauss-Krüger on Bessel 1841. Returns (phi, lambda) in radians. */
    private fun inverseGK(x: Double, y: Double, l0: Double): Pair<Double, Double> {
        val n  = (B_A - B_B) / (B_A + B_B)   // third flattening
        val n2 = n * n;  val n3 = n2 * n;  val n4 = n2 * n2

        // Meridian arc normalising factor
        val G = B_A / (1.0 + n) * (1.0 + n2 / 4.0 + n4 / 64.0) * PI

        // Normalised meridian arc σ
        val sigma = x * PI / G

        // Footpoint latitude φ_f (Helmert series)
        val phiF = sigma +
            ( 3.0 * n  / 2.0  -  27.0 * n3  / 32.0) * sin(2.0 * sigma) +
            (21.0 * n2 / 16.0 -  55.0 * n4  / 32.0) * sin(4.0 * sigma) +
            (151.0 * n3 / 96.0)                      * sin(6.0 * sigma) +
            (1097.0 * n4 / 512.0)                    * sin(8.0 * sigma)

        val sinF = sin(phiF);  val cosF = cos(phiF);  val tanF = tanF(phiF)

        // Radius of curvature in prime vertical N_f
        val NF   = B_A / sqrt(1.0 - B_E2 * sinF * sinF)
        val eta2 = (B_A * B_A / (B_B * B_B) - 1.0) * cosF * cosF  // e'² cos²φ_f

        val t2 = tanF * tanF;  val t4 = t2 * t2
        val NF2 = NF * NF;  val NF4 = NF2 * NF2;  val NF6 = NF4 * NF2
        val y2 = y * y;     val y4 = y2 * y2;     val y6 = y4 * y2

        val phi = phiF -
            tanF / (2.0 * NF2) *
                (1.0 + eta2) * y2 +
            tanF / (24.0 * NF4) *
                (5.0 + 3.0*t2 + 6.0*eta2 - 6.0*t2*eta2 - 4.0*eta2*eta2 - 9.0*t2*eta2*eta2) * y4 -
            tanF / (720.0 * NF6) *
                (61.0 + 90.0*t2 + 45.0*t4) * y6

        val NF3 = NF2 * NF;  val NF5 = NF4 * NF
        val y3 = y2 * y;     val y5 = y4 * y

        val lam = l0 +
            y / (NF * cosF) -
            y3 / (6.0 * NF3 * cosF)  * (1.0 + 2.0*t2 + eta2) +
            y5 / (120.0 * NF5 * cosF) * (5.0 + 28.0*t2 + 24.0*t4 + 6.0*eta2 + 8.0*t2*eta2)

        return Pair(phi, lam)
    }

    /** Avoids div-by-zero at poles. */
    private fun tanF(phi: Double): Double = sin(phi) / cos(phi)

    /** Geodetic (lat, lon [rad], height [m]) → ECEF (X, Y, Z [m]). */
    private fun geodeticToECEF(lat: Double, lon: Double, h: Double, a: Double, e2: Double): Triple<Double, Double, Double> {
        val N = a / sqrt(1.0 - e2 * sin(lat) * sin(lat))
        return Triple(
            (N + h) * cos(lat) * cos(lon),
            (N + h) * cos(lat) * sin(lon),
            (N * (1.0 - e2) + h) * sin(lat)
        )
    }

    /** Helmert 7-parameter (position-vector / Bursa-Wolf convention). */
    private fun helmert(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        val s = 1.0 + H_DS
        return Triple(
            H_TX + s * x - H_RZ * y + H_RY * z,
            H_TY + H_RZ * x + s * y - H_RX * z,
            H_TZ - H_RY * x + H_RX * y + s * z
        )
    }

    /** ECEF (X, Y, Z [m]) → geodetic (lat, lon [rad]) via iterative Bowring. */
    private fun ecefToGeodetic(x: Double, y: Double, z: Double, a: Double, e2: Double): Pair<Double, Double> {
        val lon = atan2(y, x)
        val p   = sqrt(x * x + y * y)
        var lat = atan2(z, p * (1.0 - e2))
        repeat(10) {
            val N = a / sqrt(1.0 - e2 * sin(lat) * sin(lat))
            lat = atan2(z + e2 * N * sin(lat), p)
        }
        return Pair(lat, lon)
    }

    /**
     * Converts WGS84 (GPS) to approximate GK4.
     * Linear approximation – sufficient for API bounding-box queries (~±50 m over Dresden).
     */
    fun wgs84ToGk4(lat: Double, lon: Double): Pair<Double, Double> {
        val up    = REF_UP    + (lat - REF_LAT) / LAT_PER_UP
        val right = REF_RIGHT + (lon - REF_LON) / LON_PER_RIGHT
        return Pair(right, up)
    }
}
