package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.OfflineCacheState
import com.example.util.OfflineMapManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineMapManagerTest {

    @Test
    fun testOfflineMapManagerInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OfflineMapManager.getInstance(context)
        assertNotNull(manager)
        assertEquals(OfflineCacheState.Idle, manager.cacheState.value)
    }

    @Test
    fun testBoundingBoxCalculation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OfflineMapManager.getInstance(context)
        val centerLat = 46.8523 // Mount Rainier
        val centerLon = -121.7603
        val radiusKm = 12.0

        val bbox = manager.createBoundingBox(centerLat, centerLon, radiusKm)
        assertNotNull(bbox)
        assertTrue(bbox.latNorth > centerLat)
        assertTrue(bbox.latSouth < centerLat)
        assertTrue(bbox.lonEast > centerLon)
        assertTrue(bbox.lonWest < centerLon)
    }

    @Test
    fun testStorageEstimation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OfflineMapManager.getInstance(context)
        val sizeMb = manager.estimateStorageSizeMb(1000)
        assertTrue("Storage size estimate should be positive", sizeMb > 0.0)
    }
}
