package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.NetworkSyncStats
import com.example.model.TripSession
import com.example.model.UserLocation
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson

@Composable
fun QuickStatsOverlay(
    activeTrip: TripSession?,
    syncStats: NetworkSyncStats,
    location: UserLocation,
    activeMembersCount: Int,
    isSosAlertActive: Boolean,
    onTripInfoClick: () -> Unit,
    onSyncBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Row: Trip Badge + Hybrid Network Sync Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trip Code Badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTripInfoClick() }
                    .testTag("trip_info_pill"),
                color = TrekDarkSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (activeTrip != null) TrekSignalGreen else TrekAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeTrip?.name ?: "Solo Tracking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (activeTrip != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "#${activeTrip.code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TrekBlazeOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Connection Mode & Peer Sync Badge
            val (modeColor, modeIcon, modeLabel) = when (syncStats.mode) {
                ConnectionMode.ONLINE_CLOUD -> Triple(TrekSignalGreen, Icons.Default.CloudDone, "Online (Cloud)")
                ConnectionMode.OFFLINE_P2P_HOTSPOT -> Triple(TrekBlazeOrange, Icons.Default.WifiTethering, "P2P Hotspot")
                ConnectionMode.OFFLINE_P2P_WIFI -> Triple(TrekAlpineCyan, Icons.Default.Wifi, "P2P Wi-Fi")
                ConnectionMode.GPS_STANDALONE -> Triple(Color.LightGray, Icons.Default.GpsFixed, "GPS Only")
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSyncBadgeClick() }
                    .testTag("sync_mode_pill"),
                color = TrekDarkSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, modeColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = modeLabel,
                        tint = modeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = modeColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Active Peers",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$activeMembersCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Telemetry Bar (Speed, Altitude, Accuracy, Distance)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telemetry_stats_bar"),
            colors = CardDefaults.cardColors(
                containerColor = TrekDarkSurface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed
                val speedKmh = location.speed * 3.6f
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "%.1f".format(speedKmh),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TrekSignalGreen
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                // Altitude
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ALTITUDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "${location.altitude.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TrekAlpineCyan
                    )
                    Text(
                        text = "meters",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                // GPS Accuracy
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACCURACY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "±${location.accuracy.toInt()}m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (location.accuracy < 10) TrekSignalGreen else TrekAmber
                    )
                    Text(
                        text = "GPS lock",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                // Trip Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL DIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    val totalM = activeTrip?.totalDistanceHikedMeters ?: 0.0
                    Text(
                        text = if (totalM > 1000) "%.1f km".format(totalM / 1000.0) else "${totalM.toInt()} m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TrekBlazeOrange
                    )
                    Text(
                        text = "tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
