package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.MemberStatus
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMemberListSheet(
    teamMembers: List<TeamMember>,
    currentLocation: UserLocation,
    onDismiss: () -> Unit,
    onFocusMember: (TeamMember) -> Unit,
    onSimulateTeammateMovement: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TrekDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("team_members_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expedition Team (${teamMembers.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time telemetry & hybrid peer discovery",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Simulate Teammate GPS Movement (helpful for multi-peer demonstration)
                Button(
                    onClick = onSimulateTeammateMovement,
                    colors = ButtonDefaults.buttonColors(containerColor = TrekBlazeOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("simulate_movement_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Simulate Move", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(teamMembers, key = { it.id }) { member ->
                    val distanceMeters = calculateDist(
                        currentLocation.latitude, currentLocation.longitude,
                        member.location.latitude, member.location.longitude
                    )
                    val bearing = calculateBearingDeg(
                        currentLocation.latitude, currentLocation.longitude,
                        member.location.latitude, member.location.longitude
                    )

                    MemberItemCard(
                        member = member,
                        distanceMeters = distanceMeters,
                        bearingDeg = bearing.toInt(),
                        onFocus = {
                            onFocusMember(member)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MemberItemCard(
    member: TeamMember,
    distanceMeters: Double,
    bearingDeg: Int,
    onFocus: () -> Unit
) {
    val memberColor = Color(android.graphics.Color.parseColor(member.colorHex))
    val isDistressed = member.isSosActive || member.status == MemberStatus.SOS_EMERGENCY

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFocus() }
            .testTag("member_card_${member.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDistressed) TrekSosCrimson.copy(alpha = 0.15f) else TrekDarkBackground
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDistressed) TrekSosCrimson else TrekDarkSurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(memberColor),
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

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (member.isLeader) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = TrekAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LEAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TrekAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatDist(distanceMeters)} away",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrekSignalGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  $bearingDeg° ${cardinalFromBearing(bearingDeg)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync badge
                    val badgeText = when (member.connectionMode) {
                        ConnectionMode.ONLINE_CLOUD -> "Cloud Relay"
                        ConnectionMode.OFFLINE_P2P_HOTSPOT -> "P2P Hotspot"
                        ConnectionMode.OFFLINE_P2P_WIFI -> "P2P Wi-Fi"
                        ConnectionMode.GPS_STANDALONE -> "Standalone"
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrekAlpineCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Alt: ${member.location.altitude.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Battery & Focus button
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (member.batteryPct > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                        contentDescription = "Battery",
                        tint = if (member.batteryPct > 20) TrekSignalGreen else TrekSosCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${member.batteryPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                IconButton(
                    onClick = onFocus,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Focus",
                        tint = TrekSignalGreen
                    )
                }
            }
        }
    }
}

private fun calculateDist(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun calculateBearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    var theta = Math.toDegrees(atan2(y, x))
    if (theta < 0) theta += 360.0
    return theta
}

private fun formatDist(meters: Double): String {
    return if (meters >= 1000) {
        "%.1f km".format(meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

private fun cardinalFromBearing(deg: Int): String {
    return when (deg) {
        in 0..22, in 338..360 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        else -> "NW"
    }
}
