package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.model.GpxRoute
import com.example.model.MapLayerType
import com.example.model.OffTrailDeviation
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekBlazeOrange
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

private const val DARK_TACTICAL_MAP_STYLE_JSON = """
[
  {
    "elementType": "geometry",
    "stylers": [{"color": "#151c24"}]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#8fa1b3"}]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [{"color": "#10161d"}]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#d0dde8"}]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#00e676"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [{"color": "#1a2c22"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#4caf50"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [{"color": "#243342"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#1b2631"}]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#b0c0cf"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [{"color": "#2c3e50"}]
  },
  {
    "featureType": "transit",
    "elementType": "geometry",
    "stylers": [{"color": "#1b2631"}]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{"color": "#0d2636"}]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#00e5ff"}]
  }
]
"""

/**
 * Production-grade Google Maps SDK integration using Jetpack Compose Google Maps.
 * Replaces the custom canvas with full-featured terrain, satellite, and tactical dark maps,
 * smooth neon GPX polyline rendering, custom teammate avatar badges, and waypoint pins.
 */
@Composable
fun TrekGoogleMapView(
    currentLocation: UserLocation,
    deviceHeading: Float,
    teamMembers: List<TeamMember>,
    waypoints: List<Waypoint>,
    activeLayer: MapLayerType,
    geofenceRadiusMeters: Float,
    gpxRoute: GpxRoute? = null,
    offTrailDeviation: OffTrailDeviation? = null,
    recenterTrigger: Int = 0,
    fitAllTrigger: Int = 0,
    directGuideMemberId: String? = null,
    selectedWaypoint: Waypoint? = null,
    onSelectMember: (TeamMember) -> Unit = {},
    onSelectWaypoint: (Waypoint) -> Unit = {},
    onDropWaypoint: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val initialLatLng = remember {
        LatLng(
            if (currentLocation.latitude != 0.0) currentLocation.latitude else 37.7749,
            if (currentLocation.longitude != 0.0) currentLocation.longitude else -122.4194
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15.8f)
    }

    // Auto-center once when real location arrives
    var hasCenteredInitially by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation.latitude, currentLocation.longitude) {
        if (!hasCenteredInitially && (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0)) {
            val userPos = LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userPos, 16.2f),
                durationMs = 800
            )
            hasCenteredInitially = true
        }
    }

    // React to Recenter Trigger
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0)) {
            val userPos = LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(userPos)
                        .zoom(16.5f)
                        .bearing(0f)
                        .tilt(0f)
                        .build()
                ),
                durationMs = 600
            )
        }
    }

    // React to Fit All Teammates Trigger
    LaunchedEffect(fitAllTrigger) {
        if (fitAllTrigger > 0 && teamMembers.isNotEmpty()) {
            val builder = LatLngBounds.builder()
            var count = 0

            if (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0) {
                builder.include(LatLng(currentLocation.latitude, currentLocation.longitude))
                count++
            }

            for (m in teamMembers) {
                builder.include(LatLng(m.location.latitude, m.location.longitude))
                count++
            }

            // Include GPX points if present
            gpxRoute?.points?.take(10)?.forEach { pt ->
                builder.include(LatLng(pt.latitude, pt.longitude))
                count++
            }

            if (count > 0) {
                try {
                    val bounds = builder.build()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120),
                        durationMs = 800
                    )
                } catch (_: Exception) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(currentLocation.latitude, currentLocation.longitude),
                            15f
                        )
                    )
                }
            }
        }
    }

    // Map Properties & Map Types
    val (mapType, mapStyleOptions) = remember(activeLayer) {
        when (activeLayer) {
            MapLayerType.OUTDOOR_TERRAIN -> Pair(MapType.TERRAIN, null)
            MapLayerType.TOPO_CONTOUR -> Pair(MapType.TERRAIN, null)
            MapLayerType.DARK_TACTICAL -> Pair(MapType.NORMAL, MapStyleOptions(DARK_TACTICAL_MAP_STYLE_JSON))
            MapLayerType.MINIMAL_TRAIL -> Pair(MapType.NORMAL, null)
        }
    }

    val mapProperties = remember(mapType, mapStyleOptions) {
        MapProperties(
            mapType = mapType,
            mapStyleOptions = mapStyleOptions,
            isMyLocationEnabled = false, // We render our own high-fidelity custom avatar marker
            isBuildingEnabled = true,
            isTrafficEnabled = false
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            compassEnabled = false, // Handled by our custom floating compass HUD
            myLocationButtonEnabled = false, // Handled by our custom floating recenter FAB
            zoomControlsEnabled = false, // Minimalist full-screen mobile gestures
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    // GPX Polyline Points cache
    val gpxLatLngList = remember(gpxRoute) {
        gpxRoute?.points?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_view"),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLongClick = { latLng ->
                onDropWaypoint(latLng.latitude, latLng.longitude)
            }
        ) {
            // 1. Geofence Perimeter Circle around Expedition Leader / User
            val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            if (currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0) {
                Circle(
                    center = userLatLng,
                    radius = geofenceRadiusMeters.toDouble(),
                    fillColor = TrekBlazeOrange.copy(alpha = 0.05f),
                    strokeColor = TrekBlazeOrange.copy(alpha = 0.55f),
                    strokeWidth = 3.5f
                )
            }

            // 2. Sleek GPX Trail Polyline (Glowing Neon Cyan with Round Caps)
            if (gpxLatLngList.size >= 2) {
                // Glow Underlay Polyline
                Polyline(
                    points = gpxLatLngList,
                    color = TrekAlpineCyan.copy(alpha = 0.3f),
                    width = 22f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )

                // Main Sleek Neon Polyline
                Polyline(
                    points = gpxLatLngList,
                    color = TrekAlpineCyan,
                    width = 10f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }

            // 3. Off-Trail Return Vector (Connects Current Location -> Closest Trail Point)
            if (offTrailDeviation?.isOffTrail == true && offTrailDeviation.nearestPoint != null) {
                val trailNearest = LatLng(
                    offTrailDeviation.nearestPoint.latitude,
                    offTrailDeviation.nearestPoint.longitude
                )

                Polyline(
                    points = listOf(userLatLng, trailNearest),
                    color = TrekSosCrimson,
                    width = 6f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )

                Circle(
                    center = trailNearest,
                    radius = 8.0,
                    fillColor = TrekSosCrimson,
                    strokeColor = Color.White,
                    strokeWidth = 3f
                )
            }

            // 4. Direct Line / Compass Guide (Connects Current Location -> Selected Teammate)
            if (directGuideMemberId != null) {
                val targetMember = teamMembers.firstOrNull { it.id == directGuideMemberId }
                if (targetMember != null) {
                    val targetLatLng = LatLng(targetMember.location.latitude, targetMember.location.longitude)
                    Polyline(
                        points = listOf(userLatLng, targetLatLng),
                        color = TrekAlpineCyan,
                        width = 8f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }
            }

            // 5. Waypoints & POIs (Camp, Water, Summit, Checkpoint)
            waypoints.forEach { wp ->
                val wpLatLng = LatLng(wp.latitude, wp.longitude)
                val isSelected = selectedWaypoint?.id == wp.id
                val markerState = rememberUpdatedMarkerState(position = wpLatLng)

                MarkerComposable(
                    keys = arrayOf<Any>(wp.id, wp.name, wp.type, isSelected),
                    state = markerState,
                    onClick = {
                        onSelectWaypoint(wp)
                        true
                    }
                ) {
                    WaypointMapMarkerPin(
                        waypoint = wp,
                        isSelected = isSelected,
                        onClick = { onSelectWaypoint(wp) }
                    )
                }
            }

            // 6. Modern Custom Teammate Markers (Avatar badge, status border, heading cone, battery pill)
            teamMembers.forEach { member ->
                val isSelf = member.id == "USER_LEADER" || member.isLeader
                val memberLatLng = LatLng(member.location.latitude, member.location.longitude)
                val heading = if (isSelf) deviceHeading else member.location.bearing
                val isDirectGuide = member.id == directGuideMemberId
                val markerState = rememberUpdatedMarkerState(position = memberLatLng)

                MarkerComposable(
                    keys = arrayOf<Any>(
                        member.id,
                        member.location.latitude,
                        member.location.longitude,
                        member.heartbeatState,
                        member.batteryPct,
                        member.isSosActive,
                        heading,
                        isDirectGuide
                    ),
                    state = markerState,
                    onClick = {
                        onSelectMember(member)
                        true
                    }
                ) {
                    TeammateMapMarkerBadge(
                        member = member,
                        heading = heading,
                        isSelf = isSelf,
                        isDirectGuideActive = isDirectGuide,
                        onClick = { onSelectMember(member) }
                    )
                }
            }
        }
    }
}
