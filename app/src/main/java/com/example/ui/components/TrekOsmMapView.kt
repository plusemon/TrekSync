package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.GpxRoute
import com.example.model.MapLayerType
import com.example.model.OffTrailDeviation
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.model.Waypoint
import org.osmdroid.views.MapView

/**
 * High-performance Zero-Key OpenStreetMap & Topographic map fallback engine.
 * Delegated to [OsmMapView].
 */
@Composable
fun TrekOsmMapView(
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
    onSelectMember: (TeamMember) -> Unit = {},
    onSelectWaypoint: (Waypoint) -> Unit = {},
    onDropWaypoint: (Double, Double) -> Unit = { _, _ -> },
    onMapReady: (MapView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    OsmMapView(
        currentLocation = currentLocation,
        deviceHeading = deviceHeading,
        teamMembers = teamMembers,
        waypoints = waypoints,
        activeLayer = activeLayer,
        geofenceRadiusMeters = geofenceRadiusMeters,
        gpxRoute = gpxRoute,
        offTrailDeviation = offTrailDeviation,
        recenterTrigger = recenterTrigger,
        fitAllTrigger = fitAllTrigger,
        directGuideMemberId = directGuideMemberId,
        onSelectMember = onSelectMember,
        onSelectWaypoint = onSelectWaypoint,
        onDropWaypoint = onDropWaypoint,
        onMapReady = onMapReady,
        modifier = modifier
    )
}
