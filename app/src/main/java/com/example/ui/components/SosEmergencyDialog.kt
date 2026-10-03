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
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SosAlert
import com.example.model.UserLocation
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import kotlinx.coroutines.delay

@Composable
fun SosEmergencyDialog(
    isSosActive: Boolean,
    currentLocation: UserLocation,
    onTriggerSos: (String) -> Unit,
    onCancelSos: () -> Unit,
    onDismiss: () -> Unit
) {
    var customMessage by remember { mutableStateOf("Immediate assistance required! Medical / Navigation emergency.") }
    var countdownSeconds by remember { mutableIntStateOf(5) }
    var isArming by remember { mutableStateOf(!isSosActive) }

    LaunchedEffect(isArming) {
        if (isArming && !isSosActive) {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
            if (countdownSeconds == 0) {
                onTriggerSos(customMessage)
                isArming = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrekDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS",
                    tint = TrekSosCrimson,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSosActive) "🚨 SOS BEACON ACTIVE" else "EMERGENCY DISTRESS BEACON",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TrekSosCrimson
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSosActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TrekSosCrimson.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, TrekSosCrimson)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BROADCASTING EMERGENCY BEACON",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TrekSosCrimson
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Transmitting high-priority distress coordinates over Cloud Relay and Local P2P Wi-Fi/Hotspot mesh.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                } else if (isArming) {
                    // Countdown Arming
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(TrekSosCrimson),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$countdownSeconds",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Broadcasting distress alert in $countdownSeconds seconds...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TrekSosCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GPS Coordinates Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrekDarkBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CURRENT GPS FIX",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lat: %.6f, Lng: %.6f".format(currentLocation.latitude, currentLocation.longitude),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TrekSignalGreen
                        )
                        Text(
                            text = "Altitude: ${currentLocation.altitude.toInt()}m • Accuracy: ±${currentLocation.accuracy.toInt()}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Emergency Note / Situation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrekSosCrimson,
                        unfocusedBorderColor = TrekDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            if (isSosActive) {
                Button(
                    onClick = {
                        onCancelSos()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrekSignalGreen),
                    modifier = Modifier.testTag("cancel_sos_beacon_btn")
                ) {
                    Text("Deactivate SOS Beacon", color = TrekDarkBackground, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onTriggerSos(customMessage)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrekSosCrimson),
                    modifier = Modifier.testTag("confirm_sos_btn")
                ) {
                    Text("Trigger SOS Now", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Dismiss")
            }
        }
    )
}
