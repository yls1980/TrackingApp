package com.trackingapp.utils

import com.trackingapp.data.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `calculateDistance should return correct distance between two points`() {
        // Moscow coordinates
        val moscowLat = 55.751244
        val moscowLon = 37.618423
        
        // Saint Petersburg coordinates
        val spbLat = 59.9311
        val spbLon = 30.3609
        
        val distance = LocationUtils.calculateDistance(moscowLat, moscowLon, spbLat, spbLon)
        
        // Distance between Moscow and SPb is approximately 635 km
        assertEquals(635000.0, distance, 10000.0) // Allow 10km tolerance
    }

    @Test
    fun `calculateTotalDistance should return correct total distance for multiple points`() {
        val points = listOf(
            Point(55.751244, 37.618423), // Moscow
            Point(55.752244, 37.619423), // ~100m away
            Point(55.753244, 37.620423)  // ~100m away from second point
        )
        
        val totalDistance = LocationUtils.calculateTotalDistance(points)
        
        // Should be approximately 200m
        assertEquals(200.0, totalDistance, 150.0) // Allow 150m tolerance for GPS inaccuracy
    }

    @Test
    fun `formatDistance should format meters correctly`() {
        assertEquals("500 м", LocationUtils.formatDistance(500.0))
        assertEquals("1.50 км", LocationUtils.formatDistance(1500.0))
        assertEquals("2.34 км", LocationUtils.formatDistance(2340.0))
    }

    @Test
    fun `formatSpeed should format speed correctly`() {
        assertEquals("3.6 км/ч", LocationUtils.formatSpeed(1.0f)) // 1 m/s = 3.6 km/h
        assertEquals("18.0 км/ч", LocationUtils.formatSpeed(5.0f)) // 5 m/s = 18 km/h
    }

    @Test
    fun `formatDuration should format duration correctly`() {
        assertEquals("30сек", LocationUtils.formatDuration(30))
        assertEquals("2мин 30сек", LocationUtils.formatDuration(150))
        assertEquals("1ч 2мин 30сек", LocationUtils.formatDuration(3750))
    }

    @Test
    fun `metersPerSecondToKmh should convert correctly`() {
        assertEquals(3.6f, LocationUtils.metersPerSecondToKmh(1.0f), 0.1f)
        assertEquals(18.0f, LocationUtils.metersPerSecondToKmh(5.0f), 0.1f)
    }

    @Test
    fun `kmhToMetersPerSecond should convert correctly`() {
        assertEquals(1.0f, LocationUtils.kmhToMetersPerSecond(3.6f), 0.1f)
        assertEquals(5.0f, LocationUtils.kmhToMetersPerSecond(18.0f), 0.1f)
    }
}



