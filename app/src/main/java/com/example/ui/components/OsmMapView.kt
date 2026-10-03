package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.GpxRoute
import com.example.model.HeartbeatState
import com.example.model.MapLayerType
import com.example.model.MemberStatus
import com.example.model.OffTrailDeviation
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

// -----------------------------------------------------------------------------
// Custom Free Tile Sources
// -----------------------------------------------------------------------------

val CARTO_DARK_TILE_SOURCE = object : OnlineTileSourceBase(
    "CartoDark",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$zoom/$x/$y.png"
    }
}

val OPEN_TOPO_TILE_SOURCE = object : OnlineTileSourceBase(
    "OpenTopoMap",
    0, 17, 256, ".png",
    arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$zoom/$x/$y.png"
    }
}

/**
 * Clean, high-performance OpenStreetMap component powered by osmdroid.
 * Features:
 * - Real world topographic/outdoor tiles with offline caching support.
 * - Teammate markers with circular avatar monogram, live battery pill, heartbeat ring, and azimuth heading cone.
 * - Glowing neon polyline GPX trail rendering.
 * - Off-trail return vector and direct-line navigational guidance lines.
 * - Compact waypoint pins.
 * - Smooth camera transitions and bounding-box fitting.
 */
@Composable
fun OsmMapView(
    currentLocation: UserLocation,
    deviceHeading: Float,
    teamMembers: List<TeamMember>,
    waypoints: List<Waypoint>,
    activeLayer: MapLayerType,
    geofenceRadiusMeters: Float,
    gpxRoute: GpxRoute? = null,
    offTrailDeviation: OffTrailDeviation? = null,
    recenterTrigger: Int = 0,
    fitAllTrigger: Int = 0,
    directGuideMemberId: String? = null,
    onSelectMember: (TeamMember) -> Unit = {},
    onSelectWaypoint: (Waypoint) -> Unit = {},
    onDropWaypoint: (Double, Double) -> Unit = { _, _ -> },
    onMapReady: (MapView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Ensure User-Agent is set
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Auto-center on user on initial launch
    var hasCenteredInitially by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation.latitude, currentLocation.longitude) {
        mapViewRef?.let { map ->
            if (!hasCenteredInitially && (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0)) {
                map.controller.setZoom(16.5)
                map.controller.animateTo(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                hasCenteredInitially = true
            }
        }
    }

    // React to Recenter Trigger
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0)) {
            mapViewRef?.let { map ->
                map.controller.setZoom(16.5)
                map.controller.animateTo(GeoPoint(currentLocation.latitude, currentLocation.longitude))
            }
        }
    }

    // React to Fit-All Trigger
    LaunchedEffect(fitAllTrigger) {
        if (fitAllTrigger > 0 && teamMembers.isNotEmpty()) {
            mapViewRef?.let { map ->
                val lats = teamMembers.map { it.location.latitude } + if (currentLocation.latitude != 0.0) listOf(currentLocation.latitude) else emptyList()
                val lngs = teamMembers.map { it.location.longitude } + if (currentLocation.longitude != 0.0) listOf(currentLocation.longitude) else emptyList()
                if (lats.isNotEmpty()) {
                    val maxLat = lats.maxOrNull() ?: currentLocation.latitude
                    val minLat = lats.minOrNull() ?: currentLocation.latitude
                    val maxLng = lngs.maxOrNull() ?: currentLocation.longitude
                    val minLng = lngs.minOrNull() ?: currentLocation.longitude

                    val boundingBox = BoundingBox(
                        maxLat + 0.002,
                        maxLng + 0.002,
                        minLat - 0.002,
                        minLng - 0.002
                    )
                    map.zoomToBoundingBox(boundingBox, true, 100)
                }
            }
        }
    }

    // Update Tile Source when activeLayer changes
    LaunchedEffect(activeLayer, mapViewRef) {
        mapViewRef?.let { map ->
            val tileSource = when (activeLayer) {
                MapLayerType.DARK_TACTICAL -> CARTO_DARK_TILE_SOURCE
                MapLayerType.TOPO_CONTOUR, MapLayerType.OUTDOOR_TERRAIN -> OPEN_TOPO_TILE_SOURCE
                MapLayerType.MINIMAL_TRAIL -> TileSourceFactory.MAPNIK
            }
            map.setTileSource(tileSource)
            map.invalidate()
        }
    }

    // Update Map Overlays (Polylines, Geofence, Waypoints, Markers)
    LaunchedEffect(
        mapViewRef,
        teamMembers,
        waypoints,
        currentLocation,
        deviceHeading,
        gpxRoute,
        offTrailDeviation,
        directGuideMemberId,
        geofenceRadiusMeters
    ) {
        mapViewRef?.let { map ->
            map.overlays.clear()

            // 1. Long-press listener overlay for dropping waypoints
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        onDropWaypoint(p.latitude, p.longitude)
                        return true
                    }
                    return false
                }
            }
            map.overlays.add(MapEventsOverlay(mapEventsReceiver))

            // 2. Geofence Circle Overlay
            if (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0) {
                val circlePoints = Polygon.pointsAsCircle(
                    GeoPoint(currentLocation.latitude, currentLocation.longitude),
                    geofenceRadiusMeters.toDouble()
                )
                val geofenceCircle = Polygon(map).apply {
                    points = circlePoints
                    fillPaint.color = android.graphics.Color.argb(20, 255, 109, 0)
                    outlinePaint.color = android.graphics.Color.argb(160, 255, 109, 0)
                    outlinePaint.strokeWidth = 4f
                }
                map.overlays.add(geofenceCircle)
            }

            // 3. GPX Trail Route (Glowing Underlay + Main Neon Cyan Polyline)
            if (gpxRoute != null && gpxRoute.points.size >= 2) {
                val gpxGeoPoints = gpxRoute.points.map { GeoPoint(it.latitude, it.longitude) }

                // Glow Underlay
                val glowLine = Polyline(map).apply {
                    setPoints(gpxGeoPoints)
                    outlinePaint.color = android.graphics.Color.argb(70, 0, 229, 255)
                    outlinePaint.strokeWidth = 18f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                map.overlays.add(glowLine)

                // Neon Line
                val trailLine = Polyline(map).apply {
                    setPoints(gpxGeoPoints)
                    outlinePaint.color = android.graphics.Color.argb(235, 0, 229, 255)
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                map.overlays.add(trailLine)
            }

            // 4. Off-Trail Return Vector
            if (offTrailDeviation?.isOffTrail == true && offTrailDeviation.nearestPoint != null) {
                val offTrailLine = Polyline(map).apply {
                    setPoints(
                        listOf(
                            GeoPoint(currentLocation.latitude, currentLocation.longitude),
                            GeoPoint(offTrailDeviation.nearestPoint.latitude, offTrailDeviation.nearestPoint.longitude)
                        )
                    )
                    outlinePaint.color = android.graphics.Color.argb(240, 255, 23, 68)
                    outlinePaint.strokeWidth = 6f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
                map.overlays.add(offTrailLine)
            }

            // 5. Direct Guide Navigation Vector
            if (directGuideMemberId != null) {
                val targetMember = teamMembers.firstOrNull { it.id == directGuideMemberId }
                if (targetMember != null) {
                    val guideLine = Polyline(map).apply {
                        setPoints(
                            listOf(
                                GeoPoint(currentLocation.latitude, currentLocation.longitude),
                                GeoPoint(targetMember.location.latitude, targetMember.location.longitude)
                            )
                        )
                        outlinePaint.color = android.graphics.Color.argb(240, 0, 229, 255)
                        outlinePaint.strokeWidth = 7f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    map.overlays.add(guideLine)
                }
            }

            // 6. Waypoint Markers
            waypoints.forEach { wp ->
                val wpMarker = Marker(map).apply {
                    position = GeoPoint(wp.latitude, wp.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createWaypointMarkerDrawable(context, wp)
                    title = wp.name
                    snippet = wp.note
                    setOnMarkerClickListener { _, _ ->
                        onSelectWaypoint(wp)
                        true
                    }
                }
                map.overlays.add(wpMarker)
            }

            // 7. Teammate Breadcrumb Trails & Markers (Avatar badge + colored status ring + heading cone + battery pill)
            teamMembers.forEach { member ->
                // Optional teammate breadcrumb trail
                if (member.breadcrumbTrail.size >= 2) {
                    val trailPoints = member.breadcrumbTrail.map { GeoPoint(it.latitude, it.longitude) }
                    val memberColorInt = try {
                        android.graphics.Color.parseColor(member.colorHex)
                    } catch (_: Exception) {
                        android.graphics.Color.rgb(0, 230, 118)
                    }
                    val breadcrumbLine = Polyline(map).apply {
                        setPoints(trailPoints)
                        outlinePaint.color = android.graphics.Color.argb(
                            100,
                            android.graphics.Color.red(memberColorInt),
                            android.graphics.Color.green(memberColorInt),
                            android.graphics.Color.blue(memberColorInt)
                        )
                        outlinePaint.strokeWidth = 4f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    map.overlays.add(breadcrumbLine)
                }

                val isSelf = member.id == "USER_LEADER" || member.isLeader
                val heading = if (isSelf) deviceHeading else member.location.bearing
                val isDirectGuide = member.id == directGuideMemberId

                val memberMarker = Marker(map).apply {
                    position = GeoPoint(member.location.latitude, member.location.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createTeammateMarkerDrawable(context, member, heading, isSelf, isDirectGuide)
                    title = member.name
                    snippet = member.callSign
                    setOnMarkerClickListener { _, _ ->
                        onSelectMember(member)
                        true
                    }
                }
                map.overlays.add(memberMarker)
            }

            map.invalidate()
        }
    }

    // Lifecycle Observer for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("osm_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(CARTO_DARK_TILE_SOURCE)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    controller.setZoom(16.5)
                    controller.setCenter(
                        GeoPoint(
                            if (currentLocation.latitude != 0.0) currentLocation.latitude else 37.7749,
                            if (currentLocation.longitude != 0.0) currentLocation.longitude else -122.4194
                        )
                    )
                    mapViewRef = this
                    onMapReady(this)
                }
            },
            update = { map ->
                mapViewRef = map
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Custom Bitmap Drawables for Map Markers
// -----------------------------------------------------------------------------

private fun createTeammateMarkerDrawable(
    context: Context,
    member: TeamMember,
    heading: Float,
    isSelf: Boolean,
    isDirectGuide: Boolean
): Drawable {
    val sizePx = 130
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    val rawColorInt = try {
        android.graphics.Color.parseColor(member.colorHex)
    } catch (_: Exception) {
        android.graphics.Color.rgb(0, 230, 118)
    }

    val isDistressed = member.isSosActive || member.status == MemberStatus.SOS_EMERGENCY
    val isLost = member.heartbeatState == HeartbeatState.LOST_CONTACT
    val isStale = member.heartbeatState == HeartbeatState.STALE

    val statusColorInt = when {
        isDistressed -> android.graphics.Color.rgb(255, 23, 68)
        isLost -> android.graphics.Color.GRAY
        isStale -> android.graphics.Color.rgb(255, 179, 0)
        else -> android.graphics.Color.rgb(0, 230, 118)
    }

    // 1. Heading Cone / Arrow
    if (!isLost) {
        canvas.save()
        canvas.rotate(heading, centerX, centerY)
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDistressed) android.graphics.Color.rgb(255, 23, 68) else (if (isSelf) android.graphics.Color.rgb(0, 229, 255) else statusColorInt)
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(centerX, centerY - 46f)
            lineTo(centerX - 10f, centerY - 28f)
            lineTo(centerX + 10f, centerY - 28f)
            close()
        }
        canvas.drawPath(path, arrowPaint)
        canvas.restore()
    }

    // 2. Outer Glow Ring for Direct Guide or SOS
    if (isDirectGuide || isDistressed) {
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDistressed) android.graphics.Color.argb(120, 255, 23, 68) else android.graphics.Color.argb(120, 0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawCircle(centerX, centerY, 32f, glowPaint)
    }

    // 3. Base Avatar Circle
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(27, 38, 59)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 24f, basePaint)

    // Inner Fill
    val innerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLost) android.graphics.Color.DKGRAY else rawColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 21f, innerFillPaint)

    // Status Border (Emerald = Active, Amber = Stale, Gray = Lost, Red = SOS)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColorInt
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
    }
    canvas.drawCircle(centerX, centerY, 23f, borderPaint)

    // Monogram / YOU
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = if (isSelf) 17f else 19f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }
    val label = if (isSelf) "YOU" else member.callSign.take(2).uppercase()
    canvas.drawText(label, centerX, centerY + 6.5f, textPaint)

    // 4. Compact Live Battery Pill under avatar
    val batteryBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        style = Paint.Style.FILL
    }
    val batteryBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(51, 65, 85)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val pillRect = RectF(centerX - 24f, centerY + 25f, centerX + 24f, centerY + 43f)
    canvas.drawRoundRect(pillRect, 8f, 8f, batteryBgPaint)
    canvas.drawRoundRect(pillRect, 8f, 8f, batteryBorderPaint)

    // Battery Dot
    val batteryDotColor = when {
        member.batteryPct > 50 -> android.graphics.Color.rgb(0, 230, 118)
        member.batteryPct > 20 -> android.graphics.Color.rgb(255, 179, 0)
        else -> android.graphics.Color.rgb(255, 23, 68)
    }
    val batteryDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = batteryDotColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX - 14f, centerY + 34f, 4f, batteryDotPaint)

    // Battery Text
    val batteryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }
    canvas.drawText("${member.batteryPct}%", centerX - 6f, centerY + 38f, batteryTextPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createWaypointMarkerDrawable(context: Context, waypoint: Waypoint): Drawable {
    val sizePx = 90
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    val colorInt = when (waypoint.type) {
        WaypointType.BASECAMP -> android.graphics.Color.rgb(0, 230, 118)
        WaypointType.WATER_SOURCE -> android.graphics.Color.rgb(0, 229, 255)
        WaypointType.CHECKPOINT -> android.graphics.Color.rgb(255, 179, 0)
        WaypointType.SUMMIT -> android.graphics.Color.rgb(192, 132, 252)
        WaypointType.DANGER_ZONE -> android.graphics.Color.rgb(255, 23, 68)
        WaypointType.RENDEZVOUS -> android.graphics.Color.rgb(255, 109, 0)
    }

    // Outer Glow
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, android.graphics.Color.red(colorInt), android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt))
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 24f, glowPaint)

    // Base Pin Circle
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 16f, basePaint)

    // Pin Core
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 11f, corePaint)

    // Center White Dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 4f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}
