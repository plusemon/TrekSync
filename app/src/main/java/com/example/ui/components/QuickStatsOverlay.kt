package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.GpxPoint
import com.example.model.GpxRoute
import com.example.model.HeartbeatState
import com.example.model.NetworkSyncStats
import com.example.model.OffTrailDeviation
import com.example.model.SosAlert
import com.example.model.TeamMember
import com.example.model.TripSession
import com.example.model.UserLocation
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekDarkSurfaceVariant
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import com.example.ui.theme.TrekTextMuted
import com.example.ui.theme.TrekTextPrimary
import com.example.ui.theme.TrekTextSecondary

// =========================================================================
// 1. TOP UNIFIED STATUS PILL (COMPACT)
// =========================================================================
@Composable
fun TopUnifiedStatusPill(
    activeTrip: TripSession?,
    syncStats: NetworkSyncStats,
    teamMembers: List<TeamMember>,
    onPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = teamMembers.count { it.heartbeatState == HeartbeatState.ACTIVE }
    val staleCount = teamMembers.count { it.heartbeatState == HeartbeatState.STALE }
    val lostCount = teamMembers.count { it.heartbeatState == HeartbeatState.LOST_CONTACT }

    val (modeColor, modeIcon, modeLabel) = when (syncStats.mode) {
        ConnectionMode.ONLINE_CLOUD -> Triple(TrekSignalGreen, Icons.Default.CloudDone, "Cloud")
        ConnectionMode.OFFLINE_P2P_HOTSPOT -> Triple(TrekBlazeOrange, Icons.Default.WifiTethering, "P2P Hub")
        ConnectionMode.OFFLINE_P2P_WIFI -> Triple(TrekAlpineCyan, Icons.Default.Wifi, "Mesh")
        ConnectionMode.GPS_STANDALONE -> Triple(Color.LightGray, Icons.Default.GpsFixed, "GPS")
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onPillClick() }
            .testTag("top_unified_status_pill"),
        color = TrekDarkSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expedition Name & Code
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (activeTrip != null) TrekSignalGreen else TrekAmber)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = activeTrip?.name ?: "TrekSync",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TrekTextPrimary
            )

            if (activeTrip != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "#${activeTrip.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrekBlazeOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(TrekDarkSurfaceBorder)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // Connection Mode Badge
            Icon(
                imageVector = modeIcon,
                contentDescription = modeLabel,
                tint = modeColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = modeColor
            )

            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(TrekDarkSurfaceBorder)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // Teammates Count Badge
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Team",
                tint = when {
                    lostCount > 0 -> TrekSosCrimson
                    staleCount > 0 -> TrekAmber
                    else -> TrekTextSecondary
                },
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$activeCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TrekTextPrimary
            )
            if (staleCount > 0 || lostCount > 0) {
                Text(
                    text = if (lostCount > 0) " (${lostCount}⚠️)" else " (${staleCount}!)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lostCount > 0) TrekSosCrimson else TrekAmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =========================================================================
// 2. COLLAPSIBLE FLOATING ALERT BADGE
// =========================================================================
@Composable
fun FloatingAlertBadge(
    isSosActive: Boolean,
    sosAlerts: List<SosAlert>,
    lostMembers: List<TeamMember>,
    staleMembers: List<TeamMember>,
    offTrailDeviation: OffTrailDeviation?,
    onAlertBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSos = isSosActive || sosAlerts.isNotEmpty()
    val hasLost = lostMembers.isNotEmpty()
    val hasStale = staleMembers.isNotEmpty()
    val isOffTrail = offTrailDeviation?.isOffTrail == true

    val hasAnyAlert = hasSos || hasLost || hasStale || isOffTrail

    AnimatedVisibility(
        visible = hasAnyAlert,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val (alertText, badgeBg, badgeIcon) = when {
            hasSos -> {
                val name = if (isSosActive) "YOUR BEACON" else sosAlerts.first().senderCallSign
                Triple("🚨 SOS ACTIVE: $name", TrekSosCrimson, Icons.Default.Warning)
            }
            hasLost -> {
                val member = lostMembers.first()
                val extra = if (isOffTrail) " • Off-Trail" else ""
                Triple("⚠️ Lost Contact: ${member.callSign}$extra", TrekSosCrimson.copy(alpha = 0.9f), Icons.Default.SignalWifiBad)
            }
            isOffTrail -> {
                val dist = offTrailDeviation?.distanceMeters ?: 0.0
                val formatted = if (dist >= 1000) String.format("%.1fkm", dist / 1000.0) else "${dist.toInt()}m"
                Triple("⚠️ Off-Trail: $formatted ${offTrailDeviation?.cardinalDirection ?: ""}", TrekAmber, Icons.Default.Warning)
            }
            hasStale -> {
                Triple("⚠️ ${staleMembers.size} Teammate Stale (>30s)", TrekAmber, Icons.Default.Warning)
            }
            else -> Triple("", TrekDarkSurface, Icons.Default.Warning)
        }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onAlertBadgeClick() }
                .testTag("floating_alert_badge"),
            color = badgeBg,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = "Alert",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = alertText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to view",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// =========================================================================
// 3. ALERT DETAILS MODAL BOTTOM SHEET
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailsBottomSheet(
    isSosActive: Boolean,
    sosAlerts: List<SosAlert>,
    lostMembers: List<TeamMember>,
    staleMembers: List<TeamMember>,
    offTrailDeviation: OffTrailDeviation?,
    gpxRoute: GpxRoute?,
    onDismissLostContact: (String) -> Unit,
    onOpenSosDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TrekDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("alert_details_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expedition Safety Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TrekTextPrimary
                    )
                    Text(
                        text = "Real-time telemetry warnings & safety status",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrekTextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TrekTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SOS Alerts
                if (isSosActive || sosAlerts.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TrekSosCrimson.copy(alpha = 0.18f)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TrekSosCrimson),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenSosDialog() }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TrekSosCrimson)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSosActive) "YOUR SOS BEACON ACTIVE" else "EMERGENCY SOS RECEIVED",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TrekSosCrimson
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isSosActive) "Broadcasting your GPS coordinates across local mesh network and cloud."
                                    else "${sosAlerts.first().senderName} (${sosAlerts.first().senderCallSign}) broadcasted distress: ${sosAlerts.first().message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TrekTextPrimary
                                )
                            }
                        }
                    }
                }

                // Off-Trail Alert
                if (offTrailDeviation?.isOffTrail == true) {
                    item {
                        val dist = offTrailDeviation.distanceMeters
                        val formatted = if (dist >= 1000) String.format("%.1f km", dist / 1000.0) else "${dist.toInt()} meters"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = TrekAmber.copy(alpha = 0.14f)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TrekAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Route, contentDescription = null, tint = TrekAmber)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Off-Trail Deviation ($formatted)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TrekAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "You are $formatted ${offTrailDeviation.cardinalDirection} from ${gpxRoute?.name ?: "the active trail"}. Follow the dashed guide vector on the map to rejoin route.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TrekTextPrimary
                                )
                            }
                        }
                    }
                }

                // Lost Contact Teammates
                items(lostMembers, key = { it.id }) { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TrekDarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TrekSosCrimson.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalWifiBad,
                                contentDescription = null,
                                tint = TrekSosCrimson,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lost Contact: ${member.name} (${member.callSign})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TrekTextPrimary
                                )
                                Text(
                                    text = "Silent for ${member.secondsSinceLastSeen}s. Last known altitude ${member.location.altitude.toInt()}m.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TrekTextSecondary
                                )
                            }
                            IconButton(onClick = { onDismissLostContact(member.id) }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = TrekTextMuted)
                            }
                        }
                    }
                }

                // Stale Teammates
                items(staleMembers, key = { it.id }) { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TrekDarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TrekAmber.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TrekAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Stale Telemetry: ${member.name} (${member.callSign})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TrekTextPrimary
                                )
                                Text(
                                    text = "No heartbeat ping in ${member.secondsSinceLastSeen}s. Battery: ${member.batteryPct}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TrekTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =========================================================================
// 4. EXPANDABLE BOTTOM TELEMETRY DRAWER / TRAIL STATS CARD
// =========================================================================
@Composable
fun ExpandableTelemetryDrawer(
    activeTrip: TripSession?,
    location: UserLocation,
    gpxRoute: GpxRoute?,
    offTrailDeviation: OffTrailDeviation?,
    onGpxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .testTag("expandable_telemetry_drawer"),
        colors = CardDefaults.cardColors(
            containerColor = TrekDarkSurface.copy(alpha = 0.96f)
        ),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Drag / Expand Handle Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(TrekTextMuted.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Compact Stats Row (Altitude, Elev Gain, Distance Remaining, Speed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Altitude
                TelemetryStatItem(
                    label = "ALTITUDE",
                    value = "${location.altitude.toInt()}",
                    unit = "meters",
                    valueColor = TrekAlpineCyan
                )

                // Elevation Gain
                val eleGain = if (gpxRoute != null) gpxRoute.elevationGainMeters else (activeTrip?.elevationGainMeters ?: 0.0)
                TelemetryStatItem(
                    label = "ELEV GAIN",
                    value = "+${eleGain.toInt()}m",
                    unit = "ascent",
                    valueColor = TrekSignalGreen
                )

                // Distance
                val distVal = if (gpxRoute != null && offTrailDeviation != null && offTrailDeviation.remainingTrailDistanceMeters > 0) {
                    offTrailDeviation.remainingTrailDistanceMeters
                } else if (gpxRoute != null) {
                    gpxRoute.totalDistanceMeters
                } else {
                    activeTrip?.totalDistanceHikedMeters ?: 0.0
                }
                val distText = if (distVal >= 1000) String.format("%.1f km", distVal / 1000.0) else "${distVal.toInt()} m"
                TelemetryStatItem(
                    label = if (gpxRoute != null) "REMAINING" else "TOTAL DIST",
                    value = distText,
                    unit = if (gpxRoute != null) "on trail" else "tracked",
                    valueColor = TrekBlazeOrange
                )

                // Speed
                val speedKmh = location.speed * 3.6f
                TelemetryStatItem(
                    label = "SPEED",
                    value = String.format("%.1f", speedKmh),
                    unit = "km/h",
                    valueColor = TrekSignalGreen
                )

                // Toggle Expand Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TrekTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Telemetry & Elevation Profile Sparkline
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TrekDarkSurfaceBorder)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trail Info Banner with Quick Trigger
                    if (gpxRoute != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TrekDarkSurfaceVariant)
                                .clickable { onGpxClick() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = TrekAlpineCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = gpxRoute.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TrekTextPrimary
                                )
                            }
                            Text(
                                text = "Total ${String.format("%.1f", gpxRoute.totalDistanceMeters / 1000.0)}km • +${gpxRoute.elevationGainMeters.toInt()}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = TrekAlpineCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Elevation Profile Canvas Graph
                    Text(
                        text = "ELEVATION PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrekTextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ElevationProfileSparkline(
                        gpxRoute = gpxRoute,
                        currentAltitude = location.altitude,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Additional Telemetry Grid (Pace, Accuracy, Max Altitude, Min Altitude)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val paceMinPerKm = if (location.speed > 0.2f) (60f / (location.speed * 3.6f)) else 0f
                        val paceStr = if (paceMinPerKm in 1f..60f) String.format("%.1f min/km", paceMinPerKm) else "-- min/km"

                        Column {
                            Text(text = "PACE", style = MaterialTheme.typography.labelSmall, color = TrekTextMuted, fontSize = 9.sp)
                            Text(text = paceStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrekTextPrimary)
                        }

                        Column {
                            Text(text = "GPS ACCURACY", style = MaterialTheme.typography.labelSmall, color = TrekTextMuted, fontSize = 9.sp)
                            Text(text = "±${location.accuracy.toInt()}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrekSignalGreen)
                        }

                        Column {
                            Text(text = "MAX ALT", style = MaterialTheme.typography.labelSmall, color = TrekTextMuted, fontSize = 9.sp)
                            val maxAlt = if (gpxRoute != null && gpxRoute.maxAltitudeMeters > 0) gpxRoute.maxAltitudeMeters.toInt() else (location.altitude.toInt() + 45)
                            Text(text = "${maxAlt}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrekTextPrimary)
                        }

                        Column {
                            Text(text = "MIN ALT", style = MaterialTheme.typography.labelSmall, color = TrekTextMuted, fontSize = 9.sp)
                            val minAlt = if (gpxRoute != null && gpxRoute.minAltitudeMeters > 0) gpxRoute.minAltitudeMeters.toInt() else (location.altitude.toInt() - 20)
                            Text(text = "${minAlt}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrekTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TrekTextMuted,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = TrekTextMuted,
            fontSize = 9.sp
        )
    }
}

// -------------------------------------------------------------
// Elevation Profile Sparkline Canvas
// -------------------------------------------------------------
@Composable
private fun ElevationProfileSparkline(
    gpxRoute: GpxRoute?,
    currentAltitude: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TrekDarkSurfaceVariant)
            .border(1.dp, TrekDarkSurfaceBorder, RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            val w = size.width
            val h = size.height

            val elePoints = gpxRoute?.points?.map { it.elevationMeters }?.filter { it > 0 } ?: emptyList()
            val profile = if (elePoints.size >= 5) {
                elePoints
            } else {
                // Fallback realistic ridge curve for current trek
                listOf(currentAltitude - 20.0, currentAltitude - 10.0, currentAltitude + 15.0, currentAltitude + 35.0, currentAltitude + 20.0, currentAltitude + 40.0)
            }

            val minEle = profile.minOrNull() ?: currentAltitude
            val maxEle = profile.maxOrNull() ?: (currentAltitude + 50)
            val range = (maxEle - minEle).coerceAtLeast(10.0)

            val path = Path()
            val fillPath = Path()

            for (i in profile.indices) {
                val x = (i.toFloat() / (profile.size - 1)) * w
                val y = h - ((profile[i] - minEle) / range * (h - 14f)).toFloat() - 4f

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            // Area Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TrekSignalGreen.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            // Line
            drawPath(
                path = path,
                color = TrekSignalGreen,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Current Position Indicator dot on curve
            val currY = h - ((currentAltitude - minEle) / range * (h - 14f)).toFloat().coerceIn(4f, h - 4f)
            val currX = w * 0.45f
            drawCircle(color = TrekAlpineCyan, radius = 4.dp.toPx(), center = Offset(currX, currY))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(currX, currY))
        }
    }
}

// -------------------------------------------------------------
// Backward compatibility wrapper for QuickStatsOverlay
// -------------------------------------------------------------
@Composable
fun QuickStatsOverlay(
    activeTrip: TripSession?,
    syncStats: NetworkSyncStats,
    location: UserLocation,
    teamMembers: List<TeamMember>,
    gpxRoute: GpxRoute? = null,
    offTrailDeviation: OffTrailDeviation? = null,
    isSosAlertActive: Boolean = false,
    onTripInfoClick: () -> Unit = {},
    onSyncBadgeClick: () -> Unit = {},
    onGpxInfoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopUnifiedStatusPill(
        activeTrip = activeTrip,
        syncStats = syncStats,
        teamMembers = teamMembers,
        onPillClick = onSyncBadgeClick,
        modifier = modifier
    )
}

