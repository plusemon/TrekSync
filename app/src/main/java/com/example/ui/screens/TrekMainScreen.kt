package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MapLayerType
import com.example.model.TeamMember
import com.example.ui.components.AddWaypointDialog
import com.example.ui.components.CompassHudView
import com.example.ui.components.CreateTripDialog
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.JoinTripDialog
import com.example.ui.components.NetworkDiagnosticsDialog
import com.example.ui.components.QuickStatsOverlay
import com.example.ui.components.SosEmergencyDialog
import com.example.ui.components.TeamMemberListSheet
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekAmber
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekDarkSurfaceBorder
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import com.example.ui.viewmodel.TrekViewModel

@Composable
fun TrekMainScreen(
    viewModel: TrekViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val deviceHeading by viewModel.deviceHeading.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val sosAlerts by viewModel.sosAlerts.collectAsStateWithLifecycle()
    val syncStats by viewModel.syncStats.collectAsStateWithLifecycle()
    val isTrackingActive by viewModel.isTrackingActive.collectAsStateWithLifecycle()
    val isSosActive by viewModel.isSosActive.collectAsStateWithLifecycle()
    val activeMapLayer by viewModel.activeMapLayer.collectAsStateWithLifecycle()

    // Dialog & Sheet states
    var showTeamListSheet by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }
    var waypointDropCoords by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            viewModel.startForegroundTracking(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrekDarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Full-screen Interactive Vector & Contours Map Canvas
            InteractiveMapCanvas(
                currentLocation = currentLocation,
                deviceHeading = deviceHeading,
                teamMembers = teamMembers,
                waypoints = waypoints,
                activeLayer = activeMapLayer,
                geofenceRadiusMeters = activeTrip?.geofenceRadiusMeters ?: 500f,
                onSelectMember = { member ->
                    // Focus selected member
                },
                onDropWaypoint = { lat, lng ->
                    waypointDropCoords = Pair(lat, lng)
                    showWaypointDialog = true
                }
            )

            // 2. Top HUD: Real-time Telemetry & Hybrid Connection Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                QuickStatsOverlay(
                    activeTrip = activeTrip,
                    syncStats = syncStats,
                    location = currentLocation,
                    activeMembersCount = teamMembers.size,
                    isSosAlertActive = isSosActive || sosAlerts.isNotEmpty(),
                    onTripInfoClick = { showCreateTripDialog = true },
                    onSyncBadgeClick = { showDiagnosticsDialog = true }
                )

                // SOS Active Banner alert if any member triggered distress signal
                if (sosAlerts.isNotEmpty() || isSosActive) {
                    val alert = sosAlerts.firstOrNull()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { showSosDialog = true }
                            .testTag("sos_active_banner"),
                        color = TrekSosCrimson,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Distress",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSosActive) "🚨 YOUR SOS BEACON BROADCASTING" else "🚨 SOS ALERT: ${alert?.senderName} (${alert?.senderCallSign})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Floating HUD Elements (Compass on bottom left)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
            ) {
                CompassHudView(
                    headingDegrees = deviceHeading,
                    currentLocation = currentLocation,
                    teamMembers = teamMembers
                )
            }

            // 4. Bottom Tactical Control Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("bottom_action_bar"),
                color = TrekDarkSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team Members Drawer Button
                    IconButton(
                        onClick = { showTeamListSheet = true },
                        modifier = Modifier.testTag("open_team_members_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = TrekSignalGreen) {
                                    Text(
                                        text = "${teamMembers.size}",
                                        fontWeight = FontWeight.Bold,
                                        color = TrekDarkBackground
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Team Members",
                                tint = Color.White
                            )
                        }
                    }

                    // Drop Waypoint Button
                    IconButton(
                        onClick = {
                            waypointDropCoords = Pair(currentLocation.latitude, currentLocation.longitude)
                            showWaypointDialog = true
                        },
                        modifier = Modifier.testTag("drop_waypoint_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocation,
                            contentDescription = "Drop Waypoint",
                            tint = TrekAlpineCyan
                        )
                    }

                    // Emergency SOS Trigger Button (Center High-Vis)
                    FloatingActionButton(
                        onClick = { showSosDialog = true },
                        containerColor = if (isSosActive) TrekSignalGreen else TrekSosCrimson,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("sos_emergency_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = "SOS Emergency",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Map Layers Menu
                    Box {
                        IconButton(
                            onClick = { showLayerMenu = true },
                            modifier = Modifier.testTag("map_layers_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Map Layers",
                                tint = TrekAmber
                            )
                        }

                        DropdownMenu(
                            expanded = showLayerMenu,
                            onDismissRequest = { showLayerMenu = false },
                            modifier = Modifier.background(TrekDarkSurface)
                        ) {
                            MapLayerType.values().forEach { layer ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = layer.title,
                                            color = if (activeMapLayer == layer) TrekSignalGreen else Color.White,
                                            fontWeight = if (activeMapLayer == layer) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.switchMapLayer(layer)
                                        showLayerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Tracking Toggle (Pause / Resume)
                    IconButton(
                        onClick = {
                            if (isTrackingActive) {
                                viewModel.stopForegroundTracking(context)
                            } else {
                                viewModel.startForegroundTracking(context)
                            }
                        },
                        modifier = Modifier.testTag("tracking_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isTrackingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTrackingActive) "Pause Tracking" else "Resume Tracking",
                            tint = if (isTrackingActive) TrekSignalGreen else TrekAmber
                        )
                    }

                    // Join / Create Trip
                    IconButton(
                        onClick = { showJoinTripDialog = true },
                        modifier = Modifier.testTag("join_trip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Join Trip",
                            tint = TrekBlazeOrange
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    if (showTeamListSheet) {
        TeamMemberListSheet(
            teamMembers = teamMembers,
            currentLocation = currentLocation,
            onDismiss = { showTeamListSheet = false },
            onFocusMember = { member ->
                // Handled
            },
            onSimulateTeammateMovement = {
                viewModel.simulateTeammateMovement()
            }
        )
    }

    if (showSosDialog) {
        SosEmergencyDialog(
            isSosActive = isSosActive,
            currentLocation = currentLocation,
            onTriggerSos = { msg ->
                viewModel.triggerSosEmergency(msg)
            },
            onCancelSos = {
                viewModel.cancelSosEmergency()
            },
            onDismiss = { showSosDialog = false }
        )
    }

    if (showCreateTripDialog) {
        CreateTripDialog(
            onDismiss = { showCreateTripDialog = false },
            onCreate = { name, code, geofence ->
                viewModel.createNewTrip(name, code, geofence)
                showCreateTripDialog = false
            }
        )
    }

    if (showJoinTripDialog) {
        JoinTripDialog(
            onDismiss = { showJoinTripDialog = false },
            onJoin = { code ->
                viewModel.joinTrip(code)
                showJoinTripDialog = false
            }
        )
    }

    if (showWaypointDialog) {
        AddWaypointDialog(
            latitude = waypointDropCoords.first,
            longitude = waypointDropCoords.second,
            onDismiss = { showWaypointDialog = false },
            onAdd = { name, type, note ->
                viewModel.dropWaypoint(
                    waypointDropCoords.first,
                    waypointDropCoords.second,
                    name,
                    type,
                    note
                )
                showWaypointDialog = false
            }
        )
    }

    if (showDiagnosticsDialog) {
        NetworkDiagnosticsDialog(
            syncStats = syncStats,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }
}
