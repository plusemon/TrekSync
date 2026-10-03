package com.example.model

enum class ConnectionMode(val label: String) {
    ONLINE_CLOUD("Online (Cloud Relay)"),
    OFFLINE_P2P_HOTSPOT("Offline (P2P Hotspot LAN)"),
    OFFLINE_P2P_WIFI("Offline (Local Wi-Fi Mesh)"),
    GPS_STANDALONE("GPS Only (Searching Peers)")
}

enum class MemberStatus {
    ACTIVE,
    STATIONARY,
    PAUSED,
    SOS_EMERGENCY,
    DISCONNECTED
}

enum class HeartbeatState {
    ACTIVE,       // < 30s
    STALE,        // 30s - 60s
    LOST_CONTACT  // > 60s
}

enum class WaypointType(val label: String, val iconName: String) {
    BASECAMP("Basecamp", "camp"),
    WATER_SOURCE("Water Source", "water"),
    CHECKPOINT("Checkpoint", "flag"),
    SUMMIT("Summit / Peak", "summit"),
    DANGER_ZONE("Hazard / Danger", "warning"),
    RENDEZVOUS("Rendezvous Point", "meet")
}

enum class MapProviderEngine(val title: String) {
    OPEN_TOPO_OFFLINE("OpenTopo / OSM (Zero-Key Offline)"),
    GOOGLE_MAPS("Google Maps SDK (Cloud)")
}

enum class MapLayerType(val title: String) {
    TOPO_CONTOUR("Topographic Contours"),
    DARK_TACTICAL("Dark Tactical Night"),
    OUTDOOR_TERRAIN("Outdoor Terrain"),
    MINIMAL_TRAIL("Clean Trail Grid")
}

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class TeamMember(
    val id: String,
    val name: String,
    val callSign: String,
    val colorHex: String,
    val location: UserLocation,
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val connectionMode: ConnectionMode = ConnectionMode.ONLINE_CLOUD,
    val isLeader: Boolean = false,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val isSosActive: Boolean = false,
    val sosMessage: String? = null,
    val breadcrumbTrail: List<UserLocation> = emptyList(),
    val heartbeatState: HeartbeatState = HeartbeatState.ACTIVE,
    val secondsSinceLastSeen: Long = 0L
)

data class GpxPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
    val timestamp: Long? = null
)

data class GpxRoute(
    val name: String,
    val points: List<GpxPoint>,
    val waypoints: List<Waypoint> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val maxAltitudeMeters: Double = 0.0,
    val minAltitudeMeters: Double = 0.0
)

data class OffTrailDeviation(
    val isOffTrail: Boolean = false,
    val distanceMeters: Double = 0.0,
    val nearestPoint: GpxPoint? = null,
    val directionAngleDeg: Float = 0f,
    val cardinalDirection: String = "N",
    val remainingTrailDistanceMeters: Double = 0.0,
    val elevationGainRemainingMeters: Double = 0.0
)

data class TripSession(
    val id: String,
    val name: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis(),
    val leaderId: String,
    val description: String = "",
    val geofenceRadiusMeters: Float = 500f,
    val isActive: Boolean = true,
    val syncMode: ConnectionMode = ConnectionMode.ONLINE_CLOUD,
    val totalDistanceHikedMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val maxSpeedKmh: Float = 0f
)

data class Waypoint(
    val id: String,
    val tripId: String,
    val name: String,
    val type: WaypointType,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val note: String = "",
    val createdBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SosAlert(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderCallSign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "EMERGENCY: Immediate Assistance Requested!",
    val isResolved: Boolean = false
)

data class NetworkSyncStats(
    val mode: ConnectionMode = ConnectionMode.ONLINE_CLOUD,
    val connectedPeersCount: Int = 0,
    val pingLatencyMs: Long = 24,
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val localIpAddress: String = "127.0.0.1",
    val isHotspotActive: Boolean = false,
    val broadcastAddresses: List<String> = emptyList(),
    val isMulticastLockHeld: Boolean = false
)
