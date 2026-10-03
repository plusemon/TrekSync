package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.MapLayerType
import com.example.model.MemberStatus
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Composable
fun InteractiveMapCanvas(
    currentLocation: UserLocation,
    deviceHeading: Float,
    teamMembers: List<TeamMember>,
    waypoints: List<Waypoint>,
    activeLayer: MapLayerType,
    geofenceRadiusMeters: Float,
    onSelectMember: (TeamMember) -> Unit,
    onDropWaypoint: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Map Center (Latitude & Longitude) and Zoom level (scale)
    var centerLat by remember { mutableStateOf(currentLocation.latitude) }
    var centerLng by remember { mutableStateOf(currentLocation.longitude) }
    var zoomLevel by remember { mutableFloatStateOf(16.5f) } // Zoom scale factor
    var mapRotation by remember { mutableFloatStateOf(0f) }

    // Selected member info popup
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    // Pulse animation for SOS beacons and active user beacon
    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulseAnim.animateTo(1f, animationSpec = tween(1500))
            pulseAnim.snapTo(0f)
        }
    }

    // Auto-update center to user initially or when user moves significantly
    var hasCenteredInitially by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation.latitude, currentLocation.longitude) {
        if (!hasCenteredInitially && (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0)) {
            centerLat = currentLocation.latitude
            centerLng = currentLocation.longitude
            hasCenteredInitially = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Projection Helper: Lat/Lng -> Screen (X, Y)
        fun project(lat: Double, lng: Double): Offset {
            val scale = (1 shl zoomLevel.toInt()).toFloat() * (1f + (zoomLevel - zoomLevel.toInt())) * 256f / 360f

            val xDelta = (lng - centerLng) * scale
            // Mercator Y conversion
            val latRad = Math.toRadians(lat)
            val centerLatRad = Math.toRadians(centerLat)
            val yDelta = (ln(tan(PI / 4 + centerLatRad / 2)) - ln(tan(PI / 4 + latRad / 2))) * (scale * 180f / PI)

            val centerX = widthPx / 2f
            val centerY = heightPx / 2f

            return Offset(
                x = centerX + xDelta.toFloat(),
                y = centerY + yDelta.toFloat()
            )
        }

        // Reverse Projection: Screen (X, Y) -> Lat/Lng
        fun unproject(screenX: Float, screenY: Float): Pair<Double, Double> {
            val scale = (1 shl zoomLevel.toInt()).toFloat() * (1f + (zoomLevel - zoomLevel.toInt())) * 256f / 360f
            val centerX = widthPx / 2f
            val centerY = heightPx / 2f

            val xDelta = screenX - centerX
            val yDelta = screenY - centerY

            val lng = centerLng + (xDelta / scale)
            val centerLatRad = Math.toRadians(centerLat)
            val targetMercatorY = ln(tan(PI / 4 + centerLatRad / 2)) - (yDelta / (scale * 180f / PI))
            val latRad = 2 * kotlin.math.atan(kotlin.math.exp(targetMercatorY)) - (PI / 2)
            val lat = Math.toDegrees(latRad)

            return Pair(lat, lng)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        zoomLevel = (zoomLevel * zoom).coerceIn(12f, 21f)
                        mapRotation += rotation

                        val scale = (1 shl zoomLevel.toInt()).toFloat() * (1f + (zoomLevel - zoomLevel.toInt())) * 256f / 360f
                        centerLng -= (pan.x / scale)
                        centerLat += (pan.y / (scale * 1.5f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            zoomLevel = (zoomLevel + 1.2f).coerceAtMost(21f)
                            val (lat, lng) = unproject(tapOffset.x, tapOffset.y)
                            centerLat = lat
                            centerLng = lng
                        },
                        onLongPress = { tapOffset ->
                            val (lat, lng) = unproject(tapOffset.x, tapOffset.y)
                            onDropWaypoint(lat, lng)
                        },
                        onTap = { tapOffset ->
                            // Check if tapped near any team member marker
                            var hitMember: TeamMember? = null
                            for (m in teamMembers) {
                                val pos = project(m.location.latitude, m.location.longitude)
                                val dist = (pos - tapOffset).getDistance()
                                if (dist < 40f) {
                                    hitMember = m
                                    break
                                }
                            }
                            selectedMemberId = hitMember?.id
                            hitMember?.let { onSelectMember(it) }
                        }
                    )
                }
        ) {
            // 1. Render Map Base Layer & Topo Contours
            drawMapBase(activeLayer, widthPx, heightPx, centerLat, centerLng, zoomLevel)

            // 2. Render Geofence Perimeter Ring around Trip Leader / Center
            val userCenterPos = project(currentLocation.latitude, currentLocation.longitude)
            val metersPerPixel = (156543.03392 * cos(Math.toRadians(currentLocation.latitude)) / (1 shl zoomLevel.toInt())).toFloat()
            val radiusPx = (geofenceRadiusMeters / metersPerPixel).coerceAtLeast(10f)

            drawCircle(
                color = TrekBlazeOrange.copy(alpha = 0.08f),
                radius = radiusPx,
                center = userCenterPos
            )
            drawCircle(
                color = TrekBlazeOrange.copy(alpha = 0.45f),
                radius = radiusPx,
                center = userCenterPos,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // 3. Render Breadcrumb Trails for Team Members
            for (member in teamMembers) {
                if (member.breadcrumbTrail.isNotEmpty()) {
                    drawBreadcrumbTrail(member.breadcrumbTrail, member.colorHex, ::project)
                }
            }

            // 4. Render Waypoints & Checkpoints
            for (wp in waypoints) {
                val wpPos = project(wp.latitude, wp.longitude)
                drawWaypointMarker(wp, wpPos)
            }

            // 5. Render Team Member Markers
            for (member in teamMembers) {
                val memberPos = project(member.location.latitude, member.location.longitude)
                val isSelf = member.id == "USER_LEADER" || member.isLeader
                val heading = if (isSelf) deviceHeading else member.location.bearing

                drawTeamMemberMarker(
                    member = member,
                    position = memberPos,
                    heading = heading,
                    pulseFraction = pulseAnim.value,
                    isSelected = member.id == selectedMemberId
                )
            }
        }

        // Map Controls: Recenter, Zoom In/Out, North Align
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recenter on All / Self
            FilledIconButton(
                onClick = {
                    centerLat = currentLocation.latitude
                    centerLng = currentLocation.longitude
                    zoomLevel = 16.5f
                    mapRotation = 0f
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("map_recenter_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = TrekDarkSurface.copy(alpha = 0.9f),
                    contentColor = TrekSignalGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Recenter Map on Current Location"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zoom In
            FilledIconButton(
                onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(21f) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_in"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = TrekDarkSurface.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Zoom Out
            FilledIconButton(
                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(12f) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_out"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = TrekDarkSurface.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Selected Member Quick Popup Card
        selectedMemberId?.let { selId ->
            val member = teamMembers.firstOrNull { it.id == selId }
            if (member != null) {
                val distanceMeters = calculateDistance(
                    currentLocation.latitude, currentLocation.longitude,
                    member.location.latitude, member.location.longitude
                )

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .testTag("selected_member_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(member.colorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.callSign.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = TrekDarkBackground,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = when (member.connectionMode) {
                                        ConnectionMode.ONLINE_CLOUD -> TrekSignalGreen.copy(alpha = 0.2f)
                                        ConnectionMode.OFFLINE_P2P_HOTSPOT -> TrekAmber.copy(alpha = 0.2f)
                                        ConnectionMode.OFFLINE_P2P_WIFI -> TrekAlpineCyan.copy(alpha = 0.2f)
                                        ConnectionMode.GPS_STANDALONE -> Color.Gray.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = member.connectionMode.label.substringBefore(" "),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (member.connectionMode) {
                                            ConnectionMode.ONLINE_CLOUD -> TrekSignalGreen
                                            ConnectionMode.OFFLINE_P2P_HOTSPOT -> TrekAmber
                                            ConnectionMode.OFFLINE_P2P_WIFI -> TrekAlpineCyan
                                            ConnectionMode.GPS_STANDALONE -> Color.LightGray
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Dist: ${formatDistance(distanceMeters)} • Alt: ${member.location.altitude.toInt()}m • Speed: ${(member.location.speed * 3.6f).toInt()} km/h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TrekSignalGreen
                            )
                        }

                        // Battery
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${member.batteryPct}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (member.batteryPct < 20) TrekSosCrimson else TrekSignalGreen
                            )
                            Text(
                                text = member.callSign,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawMapBase(
    layer: MapLayerType,
    w: Float,
    h: Float,
    centerLat: Double,
    centerLng: Double,
    zoom: Float
) {
    val bgColor = when (layer) {
        MapLayerType.DARK_TACTICAL -> Color(0xFF0A140F)
        MapLayerType.TOPO_CONTOUR -> Color(0xFF0F1D16)
        MapLayerType.OUTDOOR_TERRAIN -> Color(0xFF13231B)
        MapLayerType.MINIMAL_TRAIL -> Color(0xFF0D1812)
    }

    drawRect(color = bgColor, size = Size(w, h))

    // Topographic Contours Simulation Grid
    val contourColor = Color(0xFF1B3828)
    val gridSpacing = (40f * (zoom / 15f)).coerceIn(25f, 120f)

    // Lat/Lng Grid lines
    var x = 0f
    while (x < w) {
        drawLine(
            color = contourColor.copy(alpha = 0.35f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
        x += gridSpacing
    }

    var y = 0f
    while (y < h) {
        drawLine(
            color = contourColor.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += gridSpacing
    }

    // Topo Contour Curves (Elevation Waves)
    val path = Path()
    for (i in 0..5) {
        val yOffset = h * 0.15f * (i + 1)
        path.reset()
        path.moveTo(0f, yOffset)
        path.cubicTo(
            w * 0.25f, yOffset - 35f,
            w * 0.65f, yOffset + 45f,
            w, yOffset - 20f
        )
        drawPath(
            path = path,
            color = contourColor.copy(alpha = 0.5f),
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawBreadcrumbTrail(
    trail: List<UserLocation>,
    colorHex: String,
    project: (Double, Double) -> Offset
) {
    if (trail.size < 2) return
    val trailColor = Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.7f)
    val path = Path()

    val first = project(trail.first().latitude, trail.first().longitude)
    path.moveTo(first.x, first.y)

    for (i in 1 until trail.size) {
        val next = project(trail[i].latitude, trail[i].longitude)
        path.lineTo(next.x, next.y)
    }

    drawPath(
        path = path,
        color = trailColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    )
}

private fun DrawScope.drawWaypointMarker(wp: Waypoint, position: Offset) {
    val (markerColor, badgeLetter) = when (wp.type) {
        WaypointType.BASECAMP -> Pair(TrekSignalGreen, "B")
        WaypointType.WATER_SOURCE -> Pair(TrekAlpineCyan, "W")
        WaypointType.CHECKPOINT -> Pair(TrekAmber, "C")
        WaypointType.SUMMIT -> Pair(Color(0xFFE040FB), "S")
        WaypointType.DANGER_ZONE -> Pair(TrekSosCrimson, "!")
        WaypointType.RENDEZVOUS -> Pair(TrekBlazeOrange, "R")
    }

    // Pin base
    drawCircle(color = markerColor, radius = 14.dp.toPx(), center = position)
    drawCircle(
        color = TrekDarkBackground,
        radius = 12.dp.toPx(),
        center = position
    )

    // Inner icon ring
    drawCircle(color = markerColor, radius = 9.dp.toPx(), center = position)

    // Name text
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
        drawText(wp.name, position.x, position.y + 26.dp.toPx(), paint)
    }
}

private fun DrawScope.drawTeamMemberMarker(
    member: TeamMember,
    position: Offset,
    heading: Float,
    pulseFraction: Float,
    isSelected: Boolean
) {
    val memberColor = Color(android.graphics.Color.parseColor(member.colorHex))

    // SOS Pulse Wave
    if (member.isSosActive) {
        val pulseRadius = 24.dp.toPx() + (pulseFraction * 40.dp.toPx())
        val alpha = (1f - pulseFraction).coerceIn(0f, 1f) * 0.8f
        drawCircle(
            color = TrekSosCrimson.copy(alpha = alpha),
            radius = pulseRadius,
            center = position
        )
    }

    // Directional Cone / Heading Arrow
    rotate(degrees = heading, pivot = position) {
        val conePath = Path().apply {
            moveTo(position.x, position.y - 32.dp.toPx())
            lineTo(position.x - 10.dp.toPx(), position.y - 14.dp.toPx())
            lineTo(position.x + 10.dp.toPx(), position.y - 14.dp.toPx())
            close()
        }
        drawPath(
            path = conePath,
            color = if (member.isSosActive) TrekSosCrimson else memberColor
        )
    }

    // Outer Battery / Status Ring
    val batteryColor = when {
        member.batteryPct > 50 -> TrekSignalGreen
        member.batteryPct > 20 -> TrekAmber
        else -> TrekSosCrimson
    }

    drawCircle(
        color = batteryColor,
        radius = 18.dp.toPx(),
        center = position,
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Inner Marker Circle
    drawCircle(
        color = if (isSelected) Color.White else TrekDarkSurface,
        radius = 15.dp.toPx(),
        center = position
    )
    drawCircle(
        color = memberColor,
        radius = 13.dp.toPx(),
        center = position
    )

    // Call Sign Letters (e.g. "E1", "A2")
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val label = member.callSign.take(2).uppercase()
        drawText(label, position.x, position.y + 8f, paint)

        // Name tag under marker
        val namePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
        drawText(member.name, position.x, position.y + 30.dp.toPx(), namePaint)
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "%.1f km".format(meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}
