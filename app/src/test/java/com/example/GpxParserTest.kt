package com.example

import com.example.model.WaypointType
import com.example.util.GpxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GpxParserTest {

    private val sampleGpxXml = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="TrekSync" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>Sierra Crest Test Trail</name>
  </metadata>
  <wpt lat="37.7749" lon="-122.4194">
    <ele>300.0</ele>
    <name>Camp Alpha</name>
    <desc>Campground site</desc>
    <sym>Campground</sym>
  </wpt>
  <wpt lat="37.7780" lon="-122.4150">
    <ele>450.0</ele>
    <name>Summit Ridge</name>
    <desc>Peak vista</desc>
    <sym>Summit</sym>
  </wpt>
  <trk>
    <name>Sierra Crest Test Trail</name>
    <trkseg>
      <trkpt lat="37.7749" lon="-122.4194">
        <ele>300.0</ele>
        <time>2026-10-03T08:00:00Z</time>
      </trkpt>
      <trkpt lat="37.7760" lon="-122.4170">
        <ele>380.0</ele>
        <time>2026-10-03T08:15:00Z</time>
      </trkpt>
      <trkpt lat="37.7780" lon="-122.4150">
        <ele>450.0</ele>
        <time>2026-10-03T08:30:00Z</time>
      </trkpt>
    </trkseg>
  </trk>
</gpx>"""

    @Test
    fun `parse valid GPX route extracts name, points, waypoints and stats correctly`() {
        val result = GpxParser.parseString(sampleGpxXml)
        assertTrue(result.isSuccess)
        val route = result.getOrThrow()

        assertEquals("Sierra Crest Test Trail", route.name)
        assertEquals(3, route.points.size)
        assertEquals(2, route.waypoints.size)

        // Elevation assertions
        assertEquals(300.0, route.minAltitudeMeters, 0.1)
        assertEquals(450.0, route.maxAltitudeMeters, 0.1)
        assertEquals(150.0, route.elevationGainMeters, 0.5)
        assertEquals(0.0, route.elevationLossMeters, 0.1)

        // Distance assertions
        assertTrue("Total distance should be greater than 400m", route.totalDistanceMeters > 400.0)

        // Waypoints symbol mapping
        val campWp = route.waypoints.first { it.name == "Camp Alpha" }
        assertEquals(WaypointType.BASECAMP, campWp.type)

        val summitWp = route.waypoints.first { it.name == "Summit Ridge" }
        assertEquals(WaypointType.SUMMIT, summitWp.type)
    }

    @Test
    fun `haversine distance calculates accurate metric distance`() {
        val dist = GpxParser.haversineDistanceMeters(
            37.7749, -122.4194,
            37.7759, -122.4194
        )
        // ~111 meters for 0.001 degree latitude delta
        assertTrue("Distance should be around 111m, was $dist", dist in 105.0..118.0)
    }
}
