package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HeartbeatState
import com.example.model.MemberStatus
import com.example.model.TeamMember
import com.example.model.Waypoint
import com.example.model.WaypointType
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekDarkSurfaceVariant
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import com.example.ui.theme.TrekTextPrimary

/**
 * Custom modern Google Maps Marker for expedition teammates.
 * Features:
 * - Circular avatar badge with colored border (Active = Emerald, Stale = Amber, Lost = Muted Gray)
 * - Directional heading cone / arrow aligned with hardware compass azimuth
 * - Tiny battery pill under the avatar
 * - Clean callsign tag without raw lat/lng
 * - Pulsing SOS emergency beacon if distressed
 */
@Composable
fun TeammateMapMarkerBadge(
    member: TeamMember,
    heading: Float,
    isSelf: Boolean = false,
    isSelected: Boolean = false,
    isDirectGuideActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val rawColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (_: Exception) {
        TrekSignalGreen
    }

    val isDistressed = member.isSosActive || member.status == MemberStatus.SOS_EMERGENCY
    val isLost = member.heartbeatState == HeartbeatState.LOST_CONTACT
    val isStale = member.heartbeatState == HeartbeatState.STALE

    // Border and Accent color mapping
    val statusColor = when {
        isDistressed -> TrekSosCrimson
        isLost -> Color(0xFF9E9E9E)
        isStale -> TrekAmber
        else -> TrekSignalGreen
    }

    // Pulse animation for SOS or targeted guide
    val infiniteTransition = rememberInfiniteTransition(label = "MarkerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isDistressed || isDirectGuideActive) 1.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .testTag("teammate_marker_${member.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Directional Heading Cone / Arrow (Above Avatar)
        if (!isLost) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(heading),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Heading",
                    tint = if (isDistressed) TrekSosCrimson else (if (isSelf) TrekAlpineCyan else statusColor),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 2. Avatar Container with Status Border & Optional Pulse
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            // SOS / Guide Pulse Glow
            if (isDistressed || isDirectGuideActive) {
                Box(
                    modifier = Modifier
                        .size((44 * pulseScale).dp)
                        .clip(CircleShape)
                        .background((if (isDistressed) TrekSosCrimson else TrekAlpineCyan).copy(alpha = pulseAlpha))
                )
            }

            // Outer ring
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = TrekDarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected || isDirectGuideActive) 3.dp else 2.5.dp,
                    color = if (isSelected) Color.White else statusColor
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    rawColor.copy(alpha = if (isLost) 0.45f else 0.85f),
                                    rawColor.copy(alpha = if (isLost) 0.25f else 0.55f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelf) "YOU" else member.callSign.take(2).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = if (isSelf) 11.sp else 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Tiny Leader Star Badge or SOS Icon
            if (member.isLeader && !isSelf) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(TrekAmber)
                        .border(1.dp, TrekDarkBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★",
                        color = TrekDarkBackground,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isDistressed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(TrekSosCrimson)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 3. Tiny Battery Pill under avatar
        val batteryColor = when {
            member.batteryPct > 50 -> TrekSignalGreen
            member.batteryPct > 20 -> TrekAmber
            else -> TrekSosCrimson
        }

        Surface(
            modifier = Modifier
                .offset(y = (-4).dp)
                .shadow(4.dp, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            color = TrekDarkSurface.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, TrekDarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Charging",
                        tint = TrekAmber,
                        modifier = Modifier.size(10.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(batteryColor)
                    )
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${member.batteryPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLost) Color.Gray else TrekTextPrimary
                )
            }
        }

        // 4. Callsign Tag Pill
        Surface(
            modifier = Modifier
                .offset(y = (-2).dp)
                .shadow(4.dp, RoundedCornerShape(6.dp)),
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) TrekDarkSurfaceVariant else TrekDarkBackground.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(
                width = 0.8.dp,
                color = if (isSelected) TrekAlpineCyan else statusColor.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = member.callSign,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLost) Color.Gray else TrekTextPrimary
            )
        }
    }
}

/**
 * Custom vector pin marker for GPX Waypoints & POIs.
 */
@Composable
fun WaypointMapMarkerPin(
    waypoint: Waypoint,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val (pinColor, icon) = when (waypoint.type) {
        WaypointType.BASECAMP -> Pair(TrekSignalGreen, Icons.Default.LocationOn)
        WaypointType.WATER_SOURCE -> Pair(TrekAlpineCyan, Icons.Default.WaterDrop)
        WaypointType.CHECKPOINT -> Pair(TrekAmber, Icons.Default.Flag)
        WaypointType.SUMMIT -> Pair(Color(0xFFC084FC), Icons.Default.Landscape)
        WaypointType.DANGER_ZONE -> Pair(TrekSosCrimson, Icons.Default.Warning)
        WaypointType.RENDEZVOUS -> Pair(TrekBlazeOrange, Icons.Default.LocationOn)
    }

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .testTag("waypoint_marker_${waypoint.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(34.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            color = TrekDarkSurface,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.5.dp else 2.dp,
                color = if (isSelected) Color.White else pinColor
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(pinColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = waypoint.name,
                    tint = pinColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Optional tiny label under waypoint
        if (isSelected) {
            Surface(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
                color = TrekDarkBackground.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, pinColor)
            ) {
                Text(
                    text = waypoint.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
