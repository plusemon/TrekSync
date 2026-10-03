package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.model.GpxPoint
import com.example.model.GpxRoute
import com.example.model.Waypoint
import com.example.model.WaypointType
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GpxParser {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val isoDateFormatMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parses a GPX 1.1 formatted InputStream and extracts track points, waypoints, and stats.
     */
    fun parse(inputStream: InputStream): Result<GpxRoute> {
        return runCatching {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var routeName = "Unnamed Trail"
            val points = mutableListOf<GpxPoint>()
            val waypoints = mutableListOf<Waypoint>()

            var insideMetadata = false
            var insideTrk = false
            var insideWpt = false
            var insideTrkpt = false

            // Temp holding variables
            var currentWptLat = 0.0
            var currentWptLon = 0.0
            var currentWptEle = 0.0
            var currentWptName = ""
            var currentWptDesc = ""
            var currentWptSym = ""

            var currentTrkptLat = 0.0
            var currentTrkptLon = 0.0
            var currentTrkptEle = 0.0
            var currentTrkptTime: Long? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT) ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "metadata" -> insideMetadata = true
                            "trk" -> insideTrk = true
                            "name" -> {
                                val text = parser.nextText().trim()
                                if (insideTrk || (insideMetadata && routeName == "Unnamed Trail")) {
                                    if (text.isNotEmpty()) routeName = text
                                } else if (insideWpt) {
                                    currentWptName = text
                                }
                            }
                            "wpt" -> {
                                insideWpt = true
                                currentWptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                                currentWptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                                currentWptEle = 0.0
                                currentWptName = "Waypoint"
                                currentWptDesc = ""
                                currentWptSym = ""
                            }
                            "trkpt", "rtept" -> {
                                insideTrkpt = true
                                currentTrkptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                                currentTrkptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                                currentTrkptEle = 0.0
                                currentTrkptTime = null
                            }
                            "ele" -> {
                                val eleVal = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                                if (insideTrkpt) {
                                    currentTrkptEle = eleVal
                                } else if (insideWpt) {
                                    currentWptEle = eleVal
                                }
                            }
                            "desc" -> {
                                if (insideWpt) {
                                    currentWptDesc = parser.nextText().trim()
                                }
                            }
                            "sym" -> {
                                if (insideWpt) {
                                    currentWptSym = parser.nextText().trim()
                                }
                            }
                            "time" -> {
                                val timeStr = parser.nextText().trim()
                                if (insideTrkpt) {
                                    currentTrkptTime = parseTimestamp(timeStr)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "metadata" -> insideMetadata = false
                            "trk" -> insideTrk = false
                            "wpt" -> {
                                if (insideWpt) {
                                    val wpType = mapSymbolToWaypointType(currentWptSym, currentWptName)
                                    waypoints.add(
                                        Waypoint(
                                            id = "GPX_WP_${UUID.randomUUID().toString().take(8)}",
                                            tripId = "IMPORTED_GPX",
                                            name = currentWptName.ifEmpty { "Waypoint ${waypoints.size + 1}" },
                                            type = wpType,
                                            latitude = currentWptLat,
                                            longitude = currentWptLon,
                                            altitude = currentWptEle,
                                            note = currentWptDesc.ifEmpty { "Imported from GPX file ($currentWptSym)" },
                                            createdBy = "GPX Import",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    insideWpt = false
                                }
                            }
                            "trkpt", "rtept" -> {
                                if (insideTrkpt) {
                                    points.add(
                                        GpxPoint(
                                            latitude = currentTrkptLat,
                                            longitude = currentTrkptLon,
                                            elevationMeters = currentTrkptEle,
                                            timestamp = currentTrkptTime
                                        )
                                    )
                                    insideTrkpt = false
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (points.isEmpty() && waypoints.isEmpty()) {
                throw XmlPullParserException("No valid track points or waypoints found in GPX file.")
            }

            val stats = calculateStats(points)

            GpxRoute(
                name = routeName,
                points = points,
                waypoints = waypoints,
                totalDistanceMeters = stats.totalDistanceMeters,
                elevationGainMeters = stats.elevationGainMeters,
                elevationLossMeters = stats.elevationLossMeters,
                maxAltitudeMeters = stats.maxAltitudeMeters,
                minAltitudeMeters = stats.minAltitudeMeters
            )
        }
    }

    /**
     * Parses GPX from a raw XML String.
     */
    fun parseString(xmlContent: String): Result<GpxRoute> {
        return ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)).use { stream ->
            parse(stream)
        }
    }

    /**
     * Parses GPX from a content Uri via Android ContentResolver.
     */
    fun parseUri(context: Context, uri: Uri): Result<GpxRoute> {
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open stream for Uri: $uri")
            inputStream.use { stream ->
                parse(stream).getOrThrow()
            }
        }
    }

    private fun parseTimestamp(timeString: String): Long? {
        return try {
            isoDateFormat.parse(timeString)?.time
        } catch (_: Exception) {
            try {
                isoDateFormatMillis.parse(timeString)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun mapSymbolToWaypointType(sym: String, name: String): WaypointType {
        val combined = "${sym.lowercase(Locale.ROOT)} ${name.lowercase(Locale.ROOT)}"
        return when {
            combined.contains("camp") || combined.contains("tent") || combined.contains("shelter") -> WaypointType.BASECAMP
            combined.contains("water") || combined.contains("spring") || combined.contains("stream") || combined.contains("river") || combined.contains("lake") -> WaypointType.WATER_SOURCE
            combined.contains("summit") || combined.contains("peak") || combined.contains("top") || combined.contains("mount") -> WaypointType.SUMMIT
            combined.contains("danger") || combined.contains("hazard") || combined.contains("warning") || combined.contains("cliff") -> WaypointType.DANGER_ZONE
            combined.contains("rendezvous") || combined.contains("meet") || combined.contains("parking") || combined.contains("trailhead") -> WaypointType.RENDEZVOUS
            else -> WaypointType.CHECKPOINT
        }
    }

    private data class ParsedStats(
        val totalDistanceMeters: Double,
        val elevationGainMeters: Double,
        val elevationLossMeters: Double,
        val maxAltitudeMeters: Double,
        val minAltitudeMeters: Double
    )

    private fun calculateStats(points: List<GpxPoint>): ParsedStats {
        if (points.isEmpty()) {
            return ParsedStats(0.0, 0.0, 0.0, 0.0, 0.0)
        }

        var totalDist = 0.0
        var eleGain = 0.0
        var eleLoss = 0.0
        var maxAlt = points.first().elevationMeters
        var minAlt = points.first().elevationMeters

        for (i in 0 until points.size) {
            val pt = points[i]
            if (pt.elevationMeters > 0.0) {
                maxAlt = max(maxAlt, pt.elevationMeters)
                minAlt = if (minAlt == 0.0) pt.elevationMeters else min(minAlt, pt.elevationMeters)
            }

            if (i > 0) {
                val prev = points[i - 1]
                totalDist += haversineDistanceMeters(
                    prev.latitude, prev.longitude,
                    pt.latitude, pt.longitude
                )

                if (prev.elevationMeters > 0.0 && pt.elevationMeters > 0.0) {
                    val diff = pt.elevationMeters - prev.elevationMeters
                    if (diff > 0.5) {
                        eleGain += diff
                    } else if (diff < -0.5) {
                        eleLoss += -diff
                    }
                }
            }
        }

        return ParsedStats(
            totalDistanceMeters = totalDist,
            elevationGainMeters = eleGain,
            elevationLossMeters = eleLoss,
            maxAltitudeMeters = maxAlt,
            minAltitudeMeters = minAlt
        )
    }

    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Provides ready-to-test sample expedition GPX route for testing & offline expedition simulation.
     */
    val SAMPLE_ALPINE_CREST_GPX: String = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="TrekSync" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>Pacific Crest High Ridge Pass</name>
    <desc>Scenic high-altitude alpine ridge trail with alpine stream water sources and emergency evacuation points.</desc>
  </metadata>
  <wpt lat="37.7735" lon="-122.4210">
    <ele>275.0</ele>
    <name>Trailhead Basecamp</name>
    <desc>Expedition staging ground and emergency medical cache</desc>
    <sym>Campground</sym>
  </wpt>
  <wpt lat="37.7758" lon="-122.4182">
    <ele>310.0</ele>
    <name>Alpine Spring Water</name>
    <desc>Filtered natural mountain water source</desc>
    <sym>Water Source</sym>
  </wpt>
  <wpt lat="37.7778" lon="-122.4150">
    <ele>365.0</ele>
    <name>Granite Peak Summit</name>
    <desc>Highest summit point, panoramic view, and radio repeater</desc>
    <sym>Summit</sym>
  </wpt>
  <wpt lat="37.7760" lon="-122.4130">
    <ele>330.0</ele>
    <name>Cliff Hazard Zone</name>
    <desc>Loose scree and steep exposure: use safety harness</desc>
    <sym>Hazard</sym>
  </wpt>
  <trk>
    <name>Pacific Crest High Ridge Pass</name>
    <trkseg>
      <trkpt lat="37.7735" lon="-122.4210"><ele>275.0</ele><time>2026-10-03T08:00:00Z</time></trkpt>
      <trkpt lat="37.7742" lon="-122.4201"><ele>282.0</ele><time>2026-10-03T08:05:00Z</time></trkpt>
      <trkpt lat="37.7749" lon="-122.4194"><ele>290.0</ele><time>2026-10-03T08:10:00Z</time></trkpt>
      <trkpt lat="37.7753" lon="-122.4188"><ele>302.0</ele><time>2026-10-03T08:15:00Z</time></trkpt>
      <trkpt lat="37.7758" lon="-122.4182"><ele>310.0</ele><time>2026-10-03T08:20:00Z</time></trkpt>
      <trkpt lat="37.7765" lon="-122.4170"><ele>328.0</ele><time>2026-10-03T08:25:00Z</time></trkpt>
      <trkpt lat="37.7771" lon="-122.4160"><ele>348.0</ele><time>2026-10-03T08:30:00Z</time></trkpt>
      <trkpt lat="37.7778" lon="-122.4150"><ele>365.0</ele><time>2026-10-03T08:35:00Z</time></trkpt>
      <trkpt lat="37.7770" lon="-122.4138"><ele>345.0</ele><time>2026-10-03T08:40:00Z</time></trkpt>
      <trkpt lat="37.7760" lon="-122.4130"><ele>330.0</ele><time>2026-10-03T08:45:00Z</time></trkpt>
      <trkpt lat="37.7750" lon="-122.4120"><ele>315.0</ele><time>2026-10-03T08:50:00Z</time></trkpt>
      <trkpt lat="37.7740" lon="-122.4112"><ele>298.0</ele><time>2026-10-03T08:55:00Z</time></trkpt>
      <trkpt lat="37.7730" lon="-122.4105"><ele>280.0</ele><time>2026-10-03T09:00:00Z</time></trkpt>
    </trkseg>
  </trk>
</gpx>""".trimIndent()
}
