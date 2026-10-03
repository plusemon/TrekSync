package com.example.ui.components

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
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.WaypointType
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Power
import androidx.compose.ui.platform.LocalContext
import com.example.util.BatteryOptimizationHelper
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson

@Composable
fun CreateTripDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, geofenceMeters: Float) -> Unit
) {
    var tripName by remember { mutableStateOf("Alpine Summit Trek") }
    var tripCode by remember { mutableStateOf((1000..9999).random().toString().let { "TRK$it" }) }
    var geofenceRadius by remember { mutableFloatStateOf(500f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrekDarkSurface,
        title = {
            Text(
                text = "Create Expedition Trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Teammates can join online or zero-internet offline via this Trip Code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tripName,
                    onValueChange = { tripName = it },
                    label = { Text("Trip / Expedition Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_trip_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekSignalGreen,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tripCode,
                    onValueChange = { tripCode = it.uppercase() },
                    label = { Text("Trip Code (Share with Team)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_trip_code_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekBlazeOrange,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Geofence Perimeter Alert: ${geofenceRadius.toInt()} meters",
                    style = MaterialTheme.typography.titleSmall,
                    color = TrekSignalGreen
                )
                Slider(
                    value = geofenceRadius,
                    onValueChange = { geofenceRadius = it },
                    valueRange = 100f..3000f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = TrekSignalGreen,
                        activeTrackColor = TrekSignalGreen,
                        inactiveTrackColor = TrekDarkSurfaceBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tripName.isNotBlank() && tripCode.isNotBlank()) {
                        onCreate(tripName, tripCode, geofenceRadius)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrekSignalGreen),
                modifier = Modifier.testTag("submit_create_trip_btn")
            ) {
                Text("Start Trip", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun JoinTripDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit
) {
    var tripCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrekDarkSurface,
        title = {
            Text(
                text = "Join Expedition Trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the 6-character code provided by your Expedition Leader.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tripCode,
                    onValueChange = { tripCode = it.uppercase() },
                    label = { Text("Trip Code (e.g. TRK882)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_trip_code_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekBlazeOrange,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrekDarkBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Offline P2P Auto-Join",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TrekAlpineCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If traveling without internet, connect to the leader's Portable Hotspot or local Wi-Fi. TrekSync will automatically discover all active peers on the same trip code!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tripCode.isNotBlank()) {
                        onJoin(tripCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrekBlazeOrange),
                modifier = Modifier.testTag("submit_join_trip_btn")
            ) {
                Text("Join Team", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun AddWaypointDialog(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onAdd: (name: String, type: WaypointType, note: String) -> Unit
) {
    var name by remember { mutableStateOf("Checkpoint 1") }
    var selectedType by remember { mutableStateOf(WaypointType.CHECKPOINT) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrekDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddLocation,
                    contentDescription = null,
                    tint = TrekSignalGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Drop Trail Waypoint",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Lat: %.5f, Lng: %.5f".format(latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = TrekSignalGreen
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Waypoint Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("waypoint_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekSignalGreen,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Category / Marker Type:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Waypoint category chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WaypointType.values().take(3).forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = type },
                            color = if (isSelected) TrekSignalGreen else TrekDarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) TrekSignalGreen else TrekDarkSurfaceBorder
                            )
                        ) {
                            Text(
                                text = type.label.substringBefore(" "),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TrekDarkBackground else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WaypointType.values().drop(3).forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = type },
                            color = if (isSelected) TrekBlazeOrange else TrekDarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) TrekBlazeOrange else TrekDarkSurfaceBorder
                            )
                        ) {
                            Text(
                                text = type.label.substringBefore(" "),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TrekDarkBackground else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Hazards / Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekSignalGreen,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, selectedType, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrekSignalGreen)
            ) {
                Text("Save Waypoint", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun NetworkDiagnosticsDialog(
    syncStats: NetworkSyncStats,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isBatteryOptIgnored by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrekDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = null,
                    tint = TrekAlpineCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hybrid Mesh Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrekDarkBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Current Mode:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(text = syncStats.mode.label, color = TrekSignalGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Discovered Peers:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${syncStats.connectedPeersCount} nodes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Local IP Address:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(text = syncStats.localIpAddress, color = TrekAlpineCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subnet Broadcasts:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (syncStats.broadcastAddresses.isNotEmpty()) syncStats.broadcastAddresses.joinToString(", ") else "255.255.255.255",
                                color = TrekAlpineCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Multicast Group:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "239.255.42.99:45454", color = TrekAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Multicast Lock:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (syncStats.isMulticastLockHeld) "HELD (Radio Active)" else "STANDBY",
                                color = if (syncStats.isMulticastLockHeld) TrekSignalGreen else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Telemetry Packets:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "TX: ${syncStats.packetsSent} | RX: ${syncStats.packetsReceived}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Battery Optimization Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrekDarkBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Doze Protection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isBatteryOptIgnored) "Exempted (Zero Throttling)" else "Not Exempted (May Throttle)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryOptIgnored) TrekSignalGreen else TrekAmber
                            )
                        }
                        if (!isBatteryOptIgnored) {
                            Button(
                                onClick = {
                                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                                    isBatteryOptIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TrekAmber),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Exempt", color = TrekDarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 How Zero-Internet Tracking Works:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrekBlazeOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When internet drops in the wild, TrekSync transmits dual-layer subnet broadcast and UDP multicast packets over Wi-Fi Hotspots or local LAN. Hardware MulticastLock and WakeLocks ensure 100% reliable telemetry even with the screen locked!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TrekSignalGreen)
            ) {
                Text("Close", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Offline Map Pre-Caching Dialog for Remote Backcountry Expeditions.
 */
@Composable
fun OfflineMapCacheDialog(
    mapView: org.osmdroid.views.MapView?,
    centerLatitude: Double,
    centerLongitude: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mapManager = remember { com.example.util.OfflineMapManager.getInstance(context) }
    var radiusKm by remember { mutableFloatStateOf(10f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var progressStatus by remember { mutableStateOf("Ready to pre-cache tiles for zero-internet expeditions.") }
    var isComplete by remember { mutableStateOf(false) }
    var estimatedTiles by remember {
        mutableStateOf(
            if (mapView != null) {
                val bbox = mapManager.createBoundingBox(centerLatitude, centerLongitude, 10.0)
                mapManager.estimateTileCount(mapView, bbox, 12, 16)
            } else 240
        )
    }
    val storageEstimateMb = remember(estimatedTiles) {
        mapManager.estimateStorageSizeMb(estimatedTiles)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        containerColor = TrekDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddLocation,
                    contentDescription = null,
                    tint = TrekAlpineCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Map Tile Cache",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pre-download detailed OpenStreetMap topographic tiles for your expedition bounding box before departing into zero-connectivity terrain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrekDarkBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Center GPS:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(text = "%.4f, %.4f".format(centerLatitude, centerLongitude), color = TrekSignalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Zoom Range:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(text = "L12 - L16 (Topographic)", color = TrekAlpineCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Estimated Tiles:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(text = "$estimatedTiles tiles (~${String.format("%.1f", storageEstimateMb)} MB)", color = TrekBlazeOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isDownloading && !isComplete) {
                    Text(
                        text = "Expedition Cache Radius: ${radiusKm.toInt()} km",
                        style = MaterialTheme.typography.titleSmall,
                        color = TrekAlpineCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = radiusKm,
                        onValueChange = {
                            radiusKm = it
                            if (mapView != null) {
                                val bbox = mapManager.createBoundingBox(centerLatitude, centerLongitude, it.toDouble())
                                estimatedTiles = mapManager.estimateTileCount(mapView, bbox, 12, 16)
                            }
                        },
                        valueRange = 5f..30f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = TrekAlpineCyan,
                            activeTrackColor = TrekAlpineCyan,
                            inactiveTrackColor = TrekDarkSurfaceBorder
                        )
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = TrekAlpineCyan,
                        trackColor = TrekDarkSurfaceBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = progressStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isComplete) TrekSignalGreen else Color.Gray
                )
            }
        },
        confirmButton = {
            if (!isDownloading && !isComplete) {
                Button(
                    onClick = {
                        if (mapView != null) {
                            isDownloading = true
                            progressStatus = "Downloading map tiles for offline expedition..."
                            val bbox = mapManager.createBoundingBox(centerLatitude, centerLongitude, radiusKm.toDouble())
                            mapManager.preCacheBoundingBox(
                                mapView = mapView,
                                boundingBox = bbox,
                                minZoom = 12,
                                maxZoom = 16,
                                onComplete = { success ->
                                    isDownloading = false
                                    if (success) {
                                        isComplete = true
                                        progressStatus = "✅ Successfully cached tiles! Ready for offline expedition."
                                    } else {
                                        progressStatus = "⚠️ Pre-caching failed or was cancelled."
                                    }
                                }
                            )
                        } else {
                            progressStatus = "Map instance initializing. Try again in a moment."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrekAlpineCyan),
                    modifier = Modifier.testTag("start_offline_cache_btn")
                ) {
                    Text("Download Offline Map", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
                }
            } else if (isComplete) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TrekSignalGreen)
                ) {
                    Text("Done", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (isDownloading) {
                OutlinedButton(
                    onClick = {
                        mapManager.cancelActivePreCache()
                        isDownloading = false
                        progressStatus = "Cancelled tile download."
                    }
                ) {
                    Text("Cancel Download", color = Color.White)
                }
            } else {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    )
}

