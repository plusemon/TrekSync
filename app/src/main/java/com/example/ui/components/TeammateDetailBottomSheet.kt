package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.HeartbeatState
import com.example.model.MemberStatus
import com.example.model.TeamMember
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Elegant Material 3 Modal Bottom Sheet displaying detailed telemetry and navigation options
 * when a user clicks on an expedition teammate marker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeammateDetailBottomSheet(
    member: TeamMember,
    currentLocation: UserLocation,
    isDirectGuideActive: Boolean,
    onToggleDirectGuide: (Boolean) -> Unit,
    onCenterOnTeammate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val distanceMeters = calculateDistance(
        currentLocation.latitude, currentLocation.longitude,
        member.location.latitude, member.location.longitude
    )
    val bearingDeg = calculateBearing(
        currentLocation.latitude, currentLocation.longitude,
        member.location.latitude, member.location.longitude
    ).toInt()
    val cardinal = cardinalFromBearing(bearingDeg)

    val rawColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (_: Exception) {
        TrekSignalGreen
    }

    val isDistressed = member.isSosActive || member.status == MemberStatus.SOS_EMERGENCY
    val isLost = member.heartbeatState == HeartbeatState.LOST_CONTACT
    val isStale = member.heartbeatState == HeartbeatState.STALE

    val statusColor = when {
        isDistressed -> TrekSosCrimson
        isLost -> Color.Gray
        isStale -> TrekAmber
        else -> TrekSignalGreen
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TrekDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("teammate_detail_sheet")
        ) {
            // Header: Avatar, Name, CallSign, Status Badge, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(rawColor, rawColor.copy(alpha = 0.7f))
                            )
                        )
                        .border(2.5.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.callSign.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TrekDarkBackground
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title & CallSign
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TrekTextPrimary
                        )
                        if (member.isLeader) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = TrekAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LEADER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TrekAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = TrekDarkSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, TrekDarkSurfaceBorder)
                        ) {
                            Text(
                                text = member.callSign,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TrekAlpineCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Heartbeat status pill
                        Surface(
                            color = statusColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val statusLabel = when {
                                isDistressed -> "🚨 SOS EMERGENCY"
                                isLost -> "Lost (${member.secondsSinceLastSeen}s)"
                                isStale -> "Stale (${member.secondsSinceLastSeen}s)"
                                else -> "● Live Active"
                            }
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_teammate_detail_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TrekTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SOS Banner if distressed
            if (isDistressed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TrekSosCrimson.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrekSosCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS Alert",
                            tint = TrekSosCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DISTRESS BEACON ACTIVE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TrekSosCrimson
                            )
                            Text(
                                text = member.sosMessage ?: "Teammate requires urgent physical assistance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TrekTextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Direct Compass Guide Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDirectGuideActive) TrekAlpineCyan.copy(alpha = 0.15f) else TrekDarkSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDirectGuideActive) TrekAlpineCyan else TrekDarkSurfaceBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isDirectGuideActive) TrekAlpineCyan else TrekTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Direct Line / Compass Guide",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirectGuideActive) TrekAlpineCyan else TrekTextPrimary
                            )
                            Text(
                                text = if (isDirectGuideActive) "Target locked: guide vector rendered on map" else "Draw direct heading line toward teammate",
                                style = MaterialTheme.typography.bodySmall,
                                color = TrekTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isDirectGuideActive,
                        onCheckedChange = { onToggleDirectGuide(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrekDarkBackground,
                            checkedTrackColor = TrekAlpineCyan,
                            uncheckedThumbColor = TrekTextMuted,
                            uncheckedTrackColor = TrekDarkSurface
                        ),
                        modifier = Modifier.testTag("toggle_direct_guide_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Card Telemetry Grid: Distance & Bearing, Speed, Altitude, Battery & Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Distance & Bearing
                TelemetryDataCard(
                    modifier = Modifier.weight(1f),
                    title = "DISTANCE & BEARING",
                    icon = Icons.Default.Explore,
                    iconColor = TrekSignalGreen,
                    mainValue = formatDistance(distanceMeters),
                    subValue = "$bearingDeg° $cardinal",
                    valueColor = TrekSignalGreen
                )

                // 2. Speed & Movement
                val speedKmh = member.location.speed * 3.6f
                TelemetryDataCard(
                    modifier = Modifier.weight(1f),
                    title = "SPEED / MOTION",
                    icon = Icons.Default.Speed,
                    iconColor = TrekAlpineCyan,
                    mainValue = String.format("%.1f", speedKmh),
                    unit = "km/h",
                    subValue = if (speedKmh > 0.5f) "Moving" else "Stationary",
                    valueColor = TrekAlpineCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. Altitude
                TelemetryDataCard(
                    modifier = Modifier.weight(1f),
                    title = "ALTITUDE",
                    icon = Icons.Default.Terrain,
                    iconColor = TrekBlazeOrange,
                    mainValue = "${member.location.altitude.toInt()}",
                    unit = "m",
                    subValue = "Elevation",
                    valueColor = TrekBlazeOrange
                )

                // 4. Battery & Link
                val (connLabel, connIcon) = when (member.connectionMode) {
                    ConnectionMode.ONLINE_CLOUD -> Pair("Cloud Relay", Icons.Default.SignalCellularAlt)
                    ConnectionMode.OFFLINE_P2P_HOTSPOT -> Pair("P2P Hotspot", Icons.Default.WifiTethering)
                    ConnectionMode.OFFLINE_P2P_WIFI -> Pair("Local Mesh", Icons.Default.Wifi)
                    ConnectionMode.GPS_STANDALONE -> Pair("Standalone", Icons.Default.Navigation)
                }
                TelemetryDataCard(
                    modifier = Modifier.weight(1f),
                    title = "BATTERY & LINK",
                    icon = if (member.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    iconColor = if (member.batteryPct > 20) TrekSignalGreen else TrekSosCrimson,
                    mainValue = "${member.batteryPct}%",
                    unit = if (member.isCharging) "⚡" else "",
                    subValue = connLabel,
                    valueColor = if (member.batteryPct > 20) TrekSignalGreen else TrekSosCrimson
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Center Map & Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onCenterOnTeammate()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrekAlpineCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("center_on_teammate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = TrekDarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus on Map",
                        fontWeight = FontWeight.Bold,
                        color = TrekDarkBackground
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder),
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Close",
                        color = TrekTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TelemetryDataCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    mainValue: String,
    subValue: String,
    valueColor: Color,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = TrekDarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = TrekTextMuted
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = valueColor
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = TrekTextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TrekTextSecondary
            )
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    var theta = Math.toDegrees(atan2(y, x))
    if (theta < 0) theta += 360.0
    return theta
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format("%.1f km", meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

private fun cardinalFromBearing(deg: Int): String {
    return when (deg) {
        in 0..22, in 338..360 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "East"
        in 113..157 -> "SE"
        in 158..202 -> "South"
        in 203..247 -> "SW"
        in 248..292 -> "West"
        else -> "NW"
    }
}
