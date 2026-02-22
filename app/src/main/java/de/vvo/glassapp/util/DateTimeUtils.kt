package de.vvo.glassapp.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for parsing and formatting VVO-specific date strings.
 * VVO API often uses the "/Date(milliseconds)/" format.
 */
object DateTimeUtils {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)

    /**
     * Parses a VVO timestamp string like "/Date(1698400000000)/" and returns
     * the time in milliseconds.
     */
    fun parseVvoDate(vvoDate: String?): Long? {
        if (vvoDate == null) return null
        return try {
            val ms = vvoDate.replace("/Date(", "").replace(")/", "").toLong()
            ms
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a VVO timestamp or a millisecond value into "HH:mm".
     */
    fun formatTime(vvoDate: String?): String {
        val ms = parseVvoDate(vvoDate) ?: return "--:--"
        return timeFormat.format(Date(ms))
    }

    /**
     * Calculates delay in minutes between real time and scheduled time.
     */
    fun calculateDelay(scheduled: String?, real: String?): Int? {
        val sMs = parseVvoDate(scheduled) ?: return null
        val rMs = parseVvoDate(real) ?: return 0
        return ((rMs - sMs) / 60000).toInt()
    }
}
