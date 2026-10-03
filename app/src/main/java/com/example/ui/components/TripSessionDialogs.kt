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
                            Text(text = "UDP Multicast Port:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "45454 (239.255.42.99)", color = TrekAmber, fontWeight = FontWeight.Bold)
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

                Text(
                    text = "💡 How Zero-Internet Tracking Works:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrekBlazeOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When internet drops in the wild, TrekSync instantly activates local UDP broadcast/multicast packets. Any devices on the same Wi-Fi router or Portable Hotspot continuously exchange high-accuracy GPS coordinates in real time!",
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
