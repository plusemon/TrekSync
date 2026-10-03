package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.HeartbeatState
import com.example.model.MapLayerType
import com.example.model.MapProviderEngine
import com.example.model.TeamMember
import com.example.model.Waypoint
import com.example.ui.components.AddWaypointDialog
import com.example.ui.components.AlertDetailsBottomSheet
import com.example.ui.components.CompassHudView
import com.example.ui.components.CreateTripDialog
import com.example.ui.components.ExpandableTelemetryDrawer
import com.example.ui.components.FloatingAlertBadge
import com.example.ui.components.GpxRouteSheet
import com.example.ui.components.JoinTripDialog
import com.example.ui.components.NetworkDiagnosticsDialog
import com.example.ui.components.OfflineMapCacheDialog
import com.example.ui.components.OsmMapView
import com.example.ui.components.SosEmergencyDialog
import com.example.ui.components.TeamMemberListSheet
import com.example.ui.components.TeammateDetailBottomSheet
import com.example.ui.components.TopUnifiedStatusPill
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
import com.example.ui.theme.TrekTextSecondary
import com.example.ui.viewmodel.TrekViewModel
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Modern outdoor expedition navigation screen built with OpenStreetMap (osmdroid).
 * Provides 100% full-screen map canvas, glassmorphic HUD pill, floating controls,
 * offline tile pre-caching, and long-press distress protection.
 */
@Composable
fun TrekMainScreen(
    viewModel: TrekViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
    val loadedGpxRoute by viewModel.loadedGpxRoute.collectAsStateWithLifecycle()
    val offTrailDeviation by viewModel.offTrailDeviation.collectAsStateWithLifecycle()
    val lostContactMembers by viewModel.lostContactMembers.collectAsStateWithLifecycle()
    val gpxImportMessage by viewModel.gpxImportMessage.collectAsStateWithLifecycle()

    val staleMembers = remember(teamMembers) {
        teamMembers.filter { it.id != viewModel.userId && it.heartbeatState == HeartbeatState.STALE }
    }

    // Camera & Action triggers
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var fitAllTrigger by remember { mutableIntStateOf(0) }
    var activeMapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Direct Guide Navigation State
    var directGuideMemberId by remember { mutableStateOf<String?>(null) }
    val guidedTeammate = remember(directGuideMemberId, teamMembers) {
        teamMembers.firstOrNull { it.id == directGuideMemberId }
    }

    // Floating UI toggles
    var showCompassHud by remember { mutableStateOf(true) }
    var showTelemetryDrawer by remember { mutableStateOf(false) }

    // Dialog & Sheet states
    var selectedTeammateForDetail by remember { mutableStateOf<TeamMember?>(null) }
    var selectedWaypointForDetail by remember { mutableStateOf<Waypoint?>(null) }
    var showTeamListSheet by remember { mutableStateOf(false) }
    var showGpxSheet by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }
    var waypointDropCoords by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var showAlertDetailsSheet by remember { mutableStateOf(false) }
    var showOfflineCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(gpxImportMessage) {
        gpxImportMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearGpxMessage()
        }
    }

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
        containerColor = TrekDarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TrekDarkSurfaceVariant,
                    contentColor = TrekTextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // =========================================================================
            // 1. FULLSCREEN OPENSTREETMAP BACKGROUND (ZERO-KEY OSMDROID ENGINE)
            // =========================================================================
            OsmMapView(
                currentLocation = currentLocation,
                deviceHeading = deviceHeading,
                teamMembers = teamMembers,
                waypoints = waypoints,
                activeLayer = activeMapLayer,
                geofenceRadiusMeters = activeTrip?.geofenceRadiusMeters ?: 600f,
                gpxRoute = loadedGpxRoute,
                offTrailDeviation = offTrailDeviation,
                recenterTrigger = recenterTrigger,
                fitAllTrigger = fitAllTrigger,
                directGuideMemberId = directGuideMemberId,
                onSelectMember = { member ->
                    selectedTeammateForDetail = member
                },
                onSelectWaypoint = { wp ->
                    selectedWaypointForDetail = wp
                    snackbarHostState.currentSnackbarData?.dismiss()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Waypoint: ${wp.name} (${wp.type.label}) - ${wp.note}")
                    }
                },
                onDropWaypoint = { lat, lng ->
                    waypointDropCoords = Pair(lat, lng)
                    showWaypointDialog = true
                },
                onMapReady = { map ->
                    activeMapViewRef = map
                }
            )

            // =========================================================================
            // 2. TOP FLOATING HEADS-UP DISPLAY (GLASSMORPHIC STATUS PILL & ALERTS)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glassmorphic Status Pill: Title • Connection Status • Member Count
                TopUnifiedStatusPill(
                    activeTrip = activeTrip,
                    syncStats = syncStats,
                    teamMembers = teamMembers,
                    onPillClick = { showDiagnosticsDialog = true }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Direct Line Navigation Guide Active Banner (If Guiding to Teammate)
                guidedTeammate?.let { target ->
                    val dist = calculateDistance(
                        currentLocation.latitude, currentLocation.longitude,
                        target.location.latitude, target.location.longitude
                    )
                    val bearing = calculateBearing(
                        currentLocation.latitude, currentLocation.longitude,
                        target.location.latitude, target.location.longitude
                    ).toInt()

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .testTag("direct_guide_banner"),
                        color = TrekDarkSurface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, TrekAlpineCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Guide",
                                tint = TrekAlpineCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GUIDING TO ${target.callSign}: ${formatDistance(dist)} • $bearing° ${cardinalFromBearing(bearing)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TrekAlpineCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { directGuideMemberId = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Guide",
                                    tint = TrekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Collapsible Alert System (SOS / Lost Contact / Off-Trail Warning)
                FloatingAlertBadge(
                    isSosActive = isSosActive,
                    sosAlerts = sosAlerts,
                    lostMembers = lostContactMembers,
                    staleMembers = staleMembers,
                    offTrailDeviation = offTrailDeviation,
                    onAlertBadgeClick = { showAlertDetailsSheet = true }
                )
            }

            // =========================================================================
            // 3. FLOATING ACTION CONTROLS (RIGHT MARGIN)
            // =========================================================================
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Map Style Switcher & Offline Caching Menu
                Box {
                    FilledIconButton(
                        onClick = { showLayerMenu = true },
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape)
                            .testTag("map_layers_menu_btn"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                            contentColor = TrekAmber
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Layers",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLayerMenu,
                        onDismissRequest = { showLayerMenu = false },
                        modifier = Modifier.background(TrekDarkSurfaceVariant)
                    ) {
                        Text(
                            text = "OFFLINE EXPEDITION MAP",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TrekAlpineCyan,
                            fontSize = 10.sp
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = TrekAlpineCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pre-Cache Offline Tiles",
                                        color = TrekAlpineCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                showLayerMenu = false
                                showOfflineCacheDialog = true
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(TrekDarkSurfaceBorder)
                        )

                        Text(
                            text = "MAP THEME / CONTOURS",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TrekAmber,
                            fontSize = 10.sp
                        )

                        MapLayerType.values().forEach { layer ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (activeMapLayer == layer) "● ${layer.title}" else "  ${layer.title}",
                                        color = if (activeMapLayer == layer) TrekSignalGreen else TrekTextPrimary,
                                        fontWeight = if (activeMapLayer == layer) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
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

                // Recenter on Me Button
                FilledIconButton(
                    onClick = { recenterTrigger++ },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .testTag("recenter_on_me_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                        contentColor = TrekSignalGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on Me",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Fit All Teammates on Map Button
                FilledIconButton(
                    onClick = { fitAllTrigger++ },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .testTag("fit_all_teammates_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                        contentColor = TrekAlpineCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Fit All Teammates",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Compass HUD Toggle Button
                FilledIconButton(
                    onClick = { showCompassHud = !showCompassHud },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .testTag("toggle_compass_hud_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                        contentColor = if (showCompassHud) TrekSignalGreen else TrekTextSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass HUD",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Drop Waypoint Pin Button
                FilledIconButton(
                    onClick = {
                        waypointDropCoords = Pair(currentLocation.latitude, currentLocation.longitude)
                        showWaypointDialog = true
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .testTag("drop_waypoint_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                        contentColor = TrekAlpineCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Drop Waypoint",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // GPX Trail Route Sheet Button
                FilledIconButton(
                    onClick = { showGpxSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape)
                        .testTag("open_gpx_sheet_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TrekDarkSurface.copy(alpha = 0.94f),
                        contentColor = if (loadedGpxRoute != null) TrekAlpineCyan else TrekTextSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = "GPX Trail Route",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // =========================================================================
            // 4. FLOATING HUD: COMPASS (BOTTOM LEFT)
            // =========================================================================
            AnimatedVisibility(
                visible = showCompassHud,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = if (showTelemetryDrawer) 200.dp else 100.dp)
            ) {
                CompassHudView(
                    headingDegrees = deviceHeading,
                    currentLocation = currentLocation,
                    teamMembers = teamMembers
                )
            }

            // =========================================================================
            // 5. FLOATING BOTTOM ACTION DOCK & EXPANDABLE TELEMETRY DRAWER
            // =========================================================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Expandable Trail Elevation / Stats Drawer
                AnimatedVisibility(
                    visible = showTelemetryDrawer,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    ExpandableTelemetryDrawer(
                        activeTrip = activeTrip,
                        location = currentLocation,
                        gpxRoute = loadedGpxRoute,
                        offTrailDeviation = offTrailDeviation,
                        onGpxClick = { showGpxSheet = true },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Clean Floating 3-Core Action Dock (Members Drawer, SOS Distress Hold Button, Stats Trigger)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .shadow(12.dp, RoundedCornerShape(26.dp))
                        .testTag("bottom_action_bar"),
                    color = TrekDarkSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrekDarkSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Members Drawer Toggle (Shows active teammate count & status badge)
                        val hasLostTeammates = teamMembers.any { it.heartbeatState == HeartbeatState.LOST_CONTACT }
                        val hasStaleTeammates = teamMembers.any { it.heartbeatState == HeartbeatState.STALE }

                        IconButton(
                            onClick = { showTeamListSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_team_members_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = when {
                                            hasLostTeammates -> TrekSosCrimson
                                            hasStaleTeammates -> TrekAmber
                                            else -> TrekSignalGreen
                                        }
                                    ) {
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
                                    tint = TrekTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 2. Centerpiece: Large Thumb-Friendly SOS Distress Button (3-second long-press)
                        HoldToActivateSosButton(
                            isSosActive = isSosActive,
                            onTriggerSos = { showSosDialog = true },
                            onCancelSos = { viewModel.cancelSosEmergency() }
                        )

                        // 3. Trail Elevation / Telemetry Stats Quick Toggle
                        IconButton(
                            onClick = { showTelemetryDrawer = !showTelemetryDrawer },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("toggle_telemetry_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Trail Elevation & Stats",
                                tint = if (showTelemetryDrawer) TrekAlpineCyan else TrekTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAL DIALOGS & BOTTOM SHEETS
    // =========================================================================

    // 1. Teammate Detail Bottom Sheet (Opened by tapping any Teammate marker on OSM Map)
    selectedTeammateForDetail?.let { member ->
        TeammateDetailBottomSheet(
            member = member,
            currentLocation = currentLocation,
            isDirectGuideActive = directGuideMemberId == member.id,
            onToggleDirectGuide = { active ->
                directGuideMemberId = if (active) member.id else null
            },
            onCenterOnTeammate = {
                recenterTrigger++
            },
            onDismiss = { selectedTeammateForDetail = null }
        )
    }

    // 2. Alert Details Bottom Sheet
    if (showAlertDetailsSheet) {
        AlertDetailsBottomSheet(
            isSosActive = isSosActive,
            sosAlerts = sosAlerts,
            lostMembers = lostContactMembers,
            staleMembers = staleMembers,
            offTrailDeviation = offTrailDeviation,
            gpxRoute = loadedGpxRoute,
            onDismissLostContact = { memberId ->
                viewModel.dismissLostContactAlert(memberId)
            },
            onOpenSosDialog = {
                showAlertDetailsSheet = false
                showSosDialog = true
            },
            onDismiss = { showAlertDetailsSheet = false }
        )
    }

    // 3. Team Member List Sheet
    if (showTeamListSheet) {
        TeamMemberListSheet(
            teamMembers = teamMembers,
            currentLocation = currentLocation,
            onDismiss = { showTeamListSheet = false },
            onFocusMember = { member ->
                selectedTeammateForDetail = member
            },
            onSimulateTeammateMovement = {
                viewModel.simulateTeammateMovement()
            }
        )
    }

    // 4. GPX Route Manager Sheet
    if (showGpxSheet) {
        GpxRouteSheet(
            loadedGpx = loadedGpxRoute,
            offTrailDeviation = offTrailDeviation,
            currentThresholdMeters = viewModel.offTrailThresholdMeters,
            onSelectFile = { uri ->
                viewModel.loadGpxFromUri(context, uri)
            },
            onLoadSampleRoute = {
                viewModel.loadSampleGpxRoute()
            },
            onClearRoute = {
                viewModel.clearLoadedGpx()
            },
            onThresholdChange = { th ->
                viewModel.offTrailThresholdMeters = th
            },
            onDismiss = { showGpxSheet = false }
        )
    }

    // 5. SOS Emergency Dialog
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

    // 6. Create Trip Dialog
    if (showCreateTripDialog) {
        CreateTripDialog(
            onDismiss = { showCreateTripDialog = false },
            onCreate = { name, code, geofence ->
                viewModel.createNewTrip(name, code, geofence)
                showCreateTripDialog = false
            }
        )
    }

    // 7. Join Trip Dialog
    if (showJoinTripDialog) {
        JoinTripDialog(
            onDismiss = { showJoinTripDialog = false },
            onJoin = { code ->
                viewModel.joinTrip(code)
                showJoinTripDialog = false
            }
        )
    }

    // 8. Add Waypoint Dialog
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

    // 9. Network Diagnostics & Peer Discovery Dialog
    if (showDiagnosticsDialog) {
        NetworkDiagnosticsDialog(
            syncStats = syncStats,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // 10. Offline Map Tile Cache Dialog
    if (showOfflineCacheDialog) {
        OfflineMapCacheDialog(
            mapView = activeMapViewRef,
            centerLatitude = currentLocation.latitude,
            centerLongitude = currentLocation.longitude,
            onDismiss = { showOfflineCacheDialog = false }
        )
    }
}

/**
 * Thumb-friendly SOS button with animated countdown circle on hold.
 */
@Composable
private fun HoldToActivateSosButton(
    isSosActive: Boolean,
    onTriggerSos: () -> Unit,
    onCancelSos: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val holdProgress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(56.dp)
            .testTag("sos_emergency_fab"),
        contentAlignment = Alignment.Center
    ) {
        // Circular progress indicator during 3s hold
        if (isHolding && !isSosActive) {
            CircularProgressIndicator(
                progress = { holdProgress.value },
                modifier = Modifier.size(56.dp),
                color = TrekSosCrimson,
                trackColor = TrekSosCrimson.copy(alpha = 0.2f),
                strokeWidth = 3.5.dp
            )
        }

        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .pointerInput(isSosActive) {
                    detectTapGestures(
                        onPress = {
                            if (isSosActive) {
                                onCancelSos()
                            } else {
                                isHolding = true
                                val animJob = coroutineScope.launch {
                                    holdProgress.snapTo(0f)
                                    holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(2200, easing = LinearEasing)
                                    )
                                }
                                val completed = tryAwaitRelease()
                                animJob.cancel()
                                if (holdProgress.value >= 0.96f) {
                                    onTriggerSos()
                                }
                                isHolding = false
                                holdProgress.snapTo(0f)
                            }
                        },
                        onTap = {
                            // Tap opens the SOS dialog for immediate manual confirmation
                            onTriggerSos()
                        }
                    )
                },
            color = if (isSosActive) TrekSignalGreen else TrekSosCrimson,
            shape = CircleShape,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = "SOS Emergency",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
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
