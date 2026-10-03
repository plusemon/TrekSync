package com.example

import com.example.util.OsmTileCacheHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmTileCacheTest {

    @Test
    fun testBoundingBoxGeneration() {
        val centerLat = 37.7749
        val centerLon = -122.4194
        val radiusKm = 10.0

        val bbox = OsmTileCacheHelper.createBoundingBox(centerLat, centerLon, radiusKm)
        assertNotNull(bbox)
        assertTrue(bbox.latNorth > centerLat)
        assertTrue(bbox.latSouth < centerLat)
        assertTrue(bbox.lonEast > centerLon)
        assertTrue(bbox.lonWest < centerLon)
    }
}
