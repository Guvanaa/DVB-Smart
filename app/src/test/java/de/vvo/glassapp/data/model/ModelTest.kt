package de.vvo.glassapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    @Test
    fun testStopModel() {
        val stop = Stop(id = "123", name = "Test Stop", place = "Dresden")
        assertEquals("123", stop.id)
        assertEquals("Test Stop", stop.name)
        assertEquals("Dresden", stop.place)
    }
}
