package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TrekSyncApplication
import com.example.data.local.TrekRepository
import com.example.location.BatteryInfo
import com.example.model.ConnectionMode
import com.example.model.MapLayerType
import com.example.model.MemberStatus
import com.example.model.NetworkSyncStats
import com.example.model.SosAlert
import com.example.model.TeamMember
import com.example.model.TripSession
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import com.example.network.PacketType
import com.example.network.TelemetryPacket
import com.example.service.TrackingForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TrekViewModel(
    private val app: TrekSyncApplication,
    private val repository: TrekRepository
) : ViewModel() {

    // Current User Profile
    var userId: String = "USER_LEADER"
        private set
    var userName: String = "Alex Rivera"
        private set
    var userCallSign: String = "LEADER-1"
        private set

    // Active Trip Session
    val activeTrip: StateFlow<TripSession?> = repository.activeTrip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTrips: StateFlow<List<TripSession>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Sensors
    private val _currentLocation = MutableStateFlow(
        UserLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 280.0,
            bearing = 45f,
            speed = 1.2f,
            accuracy = 4.5f
        )
    )
    val currentLocation: StateFlow<UserLocation> = _currentLocation.asStateFlow()

    private val _deviceHeading = MutableStateFlow(0f)
    val deviceHeading: StateFlow<Float> = _deviceHeading.asStateFlow()

    private val _batteryInfo = MutableStateFlow(BatteryInfo(95, false))
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    // Tracking Service State
    private val _isTrackingActive = MutableStateFlow(true)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    // Map Layer
    private val _activeMapLayer = MutableStateFlow(MapLayerType.TOPO_CONTOUR)
    val activeMapLayer: StateFlow<MapLayerType> = _activeMapLayer.asStateFlow()

    // Team Members (In-memory + Room backed)
    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

    // Waypoints
    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints.asStateFlow()

    // SOS Alerts
    private val _sosAlerts = MutableStateFlow<List<SosAlert>>(emptyList())
    val sosAlerts: StateFlow<List<SosAlert>> = _sosAlerts.asStateFlow()

    // Network & Peer Sync Stats
    val syncStats: StateFlow<NetworkSyncStats> = app.networkManager.syncStats

    init {
        initDefaultTrip()
        startSensorCollection()
        listenToIncomingPackets()
    }

    private fun initDefaultTrip() {
        viewModelScope.launch {
            val defaultTrip = TripSession(
                id = "TRIP_ALPINE_01",
                name = "Pacific Crest Expedition",
                code = "TREK88",
                createdAt = System.currentTimeMillis(),
                leaderId = userId,
                description = "High altitude mountain trail real-time group sync",
                geofenceRadiusMeters = 600f,
                isActive = true,
                syncMode = ConnectionMode.ONLINE_CLOUD,
                totalDistanceHikedMeters = 3420.0,
                elevationGainMeters = 412.0,
                maxSpeedKmh = 6.4f
            )
            repository.createOrJoinTrip(defaultTrip)

            // Seed expedition teammates around starting coordinates
            initSampleTeammates(defaultTrip.id, _currentLocation.value)
            initSampleWaypoints(defaultTrip.id, _currentLocation.value)
        }
    }

    private fun initSampleTeammates(tripId: String, center: UserLocation) {
        val members = listOf(
            TeamMember(
                id = "USER_LEADER",
                name = userName,
                callSign = userCallSign,
                colorHex = "#00E676",
                location = center,
                batteryPct = 94,
                isCharging = false,
                lastSeenTimestamp = System.currentTimeMillis(),
                connectionMode = ConnectionMode.ONLINE_CLOUD,
                isLeader = true,
                status = MemberStatus.ACTIVE
            ),
            TeamMember(
                id = "MEMBER_02",
                name = "Elena Rostova",
                callSign = "SIERRA-2",
                colorHex = "#00E5FF",
                location = UserLocation(
                    latitude = center.latitude + 0.0018,
                    longitude = center.longitude + 0.0012,
                    altitude = center.altitude + 25.0,
                    bearing = 60f,
                    speed = 1.4f,
                    accuracy = 5f
                ),
                batteryPct = 88,
                isCharging = false,
                lastSeenTimestamp = System.currentTimeMillis() - 4000L,
                connectionMode = ConnectionMode.OFFLINE_P2P_HOTSPOT,
                isLeader = false,
                status = MemberStatus.ACTIVE
            ),
            TeamMember(
                id = "MEMBER_03",
                name = "Marcus Vance",
                callSign = "ECHO-4",
                colorHex = "#FFAB00",
                location = UserLocation(
                    latitude = center.latitude - 0.0015,
                    longitude = center.longitude + 0.0022,
                    altitude = center.altitude - 14.0,
                    bearing = 140f,
                    speed = 0.9f,
                    accuracy = 6f
                ),
                batteryPct = 76,
                isCharging = false,
                lastSeenTimestamp = System.currentTimeMillis() - 8000L,
                connectionMode = ConnectionMode.OFFLINE_P2P_WIFI,
                isLeader = false,
                status = MemberStatus.ACTIVE
            ),
            TeamMember(
                id = "MEMBER_04",
                name = "David Chen",
                callSign = "APEX-7",
                colorHex = "#E040FB",
                location = UserLocation(
                    latitude = center.latitude + 0.0028,
                    longitude = center.longitude - 0.0019,
                    altitude = center.altitude + 42.0,
                    bearing = 310f,
                    speed = 1.8f,
                    accuracy = 4f
                ),
                batteryPct = 62,
                isCharging = false,
                lastSeenTimestamp = System.currentTimeMillis() - 12000L,
                connectionMode = ConnectionMode.ONLINE_CLOUD,
                isLeader = false,
                status = MemberStatus.ACTIVE
            )
        )
        _teamMembers.value = members
    }

    private fun initSampleWaypoints(tripId: String, center: UserLocation) {
        val defaultWaypoints = listOf(
            Waypoint(
                id = "WP_01",
                tripId = tripId,
                name = "Basecamp Alpha",
                type = WaypointType.BASECAMP,
                latitude = center.latitude - 0.0020,
                longitude = center.longitude - 0.0025,
                altitude = center.altitude - 30.0,
                note = "Shelter, tents, and medical supply cache",
                createdBy = userName
            ),
            Waypoint(
                id = "WP_02",
                tripId = tripId,
                name = "Spring Water",
                type = WaypointType.WATER_SOURCE,
                latitude = center.latitude + 0.0012,
                longitude = center.longitude + 0.0030,
                altitude = center.altitude + 15.0,
                note = "Potable mountain spring water stream",
                createdBy = "Elena Rostova"
            ),
            Waypoint(
                id = "WP_03",
                tripId = tripId,
                name = "Ridge Summit",
                type = WaypointType.SUMMIT,
                latitude = center.latitude + 0.0035,
                longitude = center.longitude + 0.0020,
                altitude = center.altitude + 85.0,
                note = "Peak panorama and emergency radio relay",
                createdBy = "Alex Rivera"
            )
        )
        _waypoints.value = defaultWaypoints
    }

    private fun startSensorCollection() {
        viewModelScope.launch {
            app.locationEngine.getLocationUpdates().collectLatest { loc ->
                _currentLocation.value = loc
                updateSelfLocation(loc)
            }
        }

        viewModelScope.launch {
            app.compassEngine.getHeadingFlow().collectLatest { heading ->
                _deviceHeading.value = heading
            }
        }

        viewModelScope.launch {
            app.batteryManager.getBatteryInfoFlow().collectLatest { info ->
                _batteryInfo.value = info
            }
        }
    }

    private fun listenToIncomingPackets() {
        viewModelScope.launch {
            app.networkManager.incomingPackets.collectLatest { packet ->
                if (packet.senderId == userId) return@collectLatest

                when (packet.type) {
                    PacketType.LOCATION_UPDATE, PacketType.HEARTBEAT_PING, PacketType.JOIN_ANNOUNCE -> {
                        updateMemberFromPacket(packet)
                    }
                    PacketType.SOS_ALERT -> {
                        updateMemberFromPacket(packet)
                        val alert = SosAlert(
                            id = UUID.randomUUID().toString(),
                            senderId = packet.senderId,
                            senderName = packet.senderName,
                            senderCallSign = packet.callSign,
                            latitude = packet.latitude,
                            longitude = packet.longitude,
                            altitude = packet.altitude,
                            timestamp = packet.timestamp,
                            message = packet.payloadMessage ?: "EMERGENCY: Distress Signal Broadcasted!"
                        )
                        _sosAlerts.value = listOf(alert) + _sosAlerts.value
                    }
                    PacketType.WAYPOINT_SHARED -> {
                        packet.waypointData?.let { wp ->
                            _waypoints.value = _waypoints.value.filterNot { it.id == wp.id } + wp
                        }
                    }
                }
            }
        }
    }

    private fun updateSelfLocation(loc: UserLocation) {
        val currentList = _teamMembers.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == userId }
        val updatedSelf = TeamMember(
            id = userId,
            name = userName,
            callSign = userCallSign,
            colorHex = "#00E676",
            location = loc.copy(bearing = _deviceHeading.value),
            batteryPct = _batteryInfo.value.percentage,
            isCharging = _batteryInfo.value.isCharging,
            lastSeenTimestamp = System.currentTimeMillis(),
            connectionMode = app.networkManager.connectionMode.value,
            isLeader = true,
            status = if (_isSosActive.value) MemberStatus.SOS_EMERGENCY else MemberStatus.ACTIVE,
            isSosActive = _isSosActive.value
        )

        if (idx >= 0) {
            currentList[idx] = updatedSelf
        } else {
            currentList.add(0, updatedSelf)
        }
        _teamMembers.value = currentList
    }

    private fun updateMemberFromPacket(packet: TelemetryPacket) {
        val currentList = _teamMembers.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == packet.senderId }
        val memberLoc = UserLocation(
            latitude = packet.latitude,
            longitude = packet.longitude,
            altitude = packet.altitude,
            bearing = packet.bearing,
            speed = packet.speed,
            accuracy = packet.accuracy,
            timestamp = packet.timestamp
        )

        val updated = if (idx >= 0) {
            val existing = currentList[idx]
            val newTrail = existing.breadcrumbTrail + memberLoc
            existing.copy(
                location = memberLoc,
                batteryPct = packet.batteryPct,
                isCharging = packet.isCharging,
                lastSeenTimestamp = packet.timestamp,
                status = packet.status,
                isSosActive = packet.type == PacketType.SOS_ALERT || packet.status == MemberStatus.SOS_EMERGENCY,
                breadcrumbTrail = if (newTrail.size > 20) newTrail.takeLast(20) else newTrail
            )
        } else {
            TeamMember(
                id = packet.senderId,
                name = packet.senderName,
                callSign = packet.callSign,
                colorHex = packet.colorHex,
                location = memberLoc,
                batteryPct = packet.batteryPct,
                isCharging = packet.isCharging,
                lastSeenTimestamp = packet.timestamp,
                connectionMode = app.networkManager.connectionMode.value,
                isLeader = false,
                status = packet.status,
                isSosActive = packet.type == PacketType.SOS_ALERT,
                breadcrumbTrail = listOf(memberLoc)
            )
        }

        if (idx >= 0) {
            currentList[idx] = updated
        } else {
            currentList.add(updated)
        }
        _teamMembers.value = currentList
    }

    fun startForegroundTracking(context: Context) {
        val trip = activeTrip.value ?: return
        TrackingForegroundService.startService(
            context = context,
            tripId = trip.id,
            tripCode = trip.code,
            userId = userId,
            userName = userName,
            callSign = userCallSign
        )
        _isTrackingActive.value = true
    }

    fun stopForegroundTracking(context: Context) {
        TrackingForegroundService.stopService(context)
        _isTrackingActive.value = false
    }

    fun triggerSosEmergency(message: String) {
        _isSosActive.value = true
        viewModelScope.launch {
            val trip = activeTrip.value
            val packet = TelemetryPacket(
                type = PacketType.SOS_ALERT,
                tripCode = trip?.code ?: "EMERGENCY",
                senderId = userId,
                senderName = userName,
                callSign = userCallSign,
                colorHex = "#FF1744",
                latitude = _currentLocation.value.latitude,
                longitude = _currentLocation.value.longitude,
                altitude = _currentLocation.value.altitude,
                bearing = _deviceHeading.value,
                speed = _currentLocation.value.speed,
                accuracy = _currentLocation.value.accuracy,
                batteryPct = _batteryInfo.value.percentage,
                isCharging = _batteryInfo.value.isCharging,
                status = MemberStatus.SOS_EMERGENCY,
                timestamp = System.currentTimeMillis(),
                payloadMessage = message
            )
            app.networkManager.broadcastTelemetry(packet)
        }
    }

    fun cancelSosEmergency() {
        _isSosActive.value = false
        viewModelScope.launch {
            val trip = activeTrip.value
            val packet = TelemetryPacket(
                type = PacketType.LOCATION_UPDATE,
                tripCode = trip?.code ?: "NORMAL",
                senderId = userId,
                senderName = userName,
                callSign = userCallSign,
                colorHex = "#00E676",
                latitude = _currentLocation.value.latitude,
                longitude = _currentLocation.value.longitude,
                altitude = _currentLocation.value.altitude,
                bearing = _deviceHeading.value,
                speed = _currentLocation.value.speed,
                accuracy = _currentLocation.value.accuracy,
                batteryPct = _batteryInfo.value.percentage,
                isCharging = _batteryInfo.value.isCharging,
                status = MemberStatus.ACTIVE,
                timestamp = System.currentTimeMillis()
            )
            app.networkManager.broadcastTelemetry(packet)
        }
    }

    fun createNewTrip(name: String, code: String, geofenceMeters: Float) {
        viewModelScope.launch {
            val newTrip = TripSession(
                id = UUID.randomUUID().toString(),
                name = name,
                code = code,
                createdAt = System.currentTimeMillis(),
                leaderId = userId,
                geofenceRadiusMeters = geofenceMeters,
                isActive = true,
                syncMode = app.networkManager.connectionMode.value
            )
            repository.createOrJoinTrip(newTrip)
            initSampleTeammates(newTrip.id, _currentLocation.value)
        }
    }

    fun joinTrip(code: String) {
        viewModelScope.launch {
            val joinedTrip = TripSession(
                id = "TRIP_$code",
                name = "Trip #$code",
                code = code,
                createdAt = System.currentTimeMillis(),
                leaderId = "EXTERNAL_LEAD",
                geofenceRadiusMeters = 500f,
                isActive = true,
                syncMode = app.networkManager.connectionMode.value
            )
            repository.createOrJoinTrip(joinedTrip)
        }
    }

    fun dropWaypoint(lat: Double, lng: Double, name: String, type: WaypointType, note: String) {
        viewModelScope.launch {
            val tripId = activeTrip.value?.id ?: "DEFAULT"
            val wp = Waypoint(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                name = name,
                type = type,
                latitude = lat,
                longitude = lng,
                altitude = _currentLocation.value.altitude,
                note = note,
                createdBy = userName
            )
            repository.addWaypoint(wp)
            _waypoints.value = listOf(wp) + _waypoints.value

            // Broadcast to all team members over P2P mesh and cloud
            val packet = TelemetryPacket(
                type = PacketType.WAYPOINT_SHARED,
                tripCode = activeTrip.value?.code ?: "",
                senderId = userId,
                senderName = userName,
                callSign = userCallSign,
                colorHex = "#00E676",
                latitude = lat,
                longitude = lng,
                altitude = _currentLocation.value.altitude,
                bearing = 0f,
                speed = 0f,
                accuracy = 1f,
                batteryPct = _batteryInfo.value.percentage,
                isCharging = _batteryInfo.value.isCharging,
                status = MemberStatus.ACTIVE,
                timestamp = System.currentTimeMillis(),
                waypointData = wp
            )
            app.networkManager.broadcastTelemetry(packet)
        }
    }

    fun switchMapLayer(layer: MapLayerType) {
        _activeMapLayer.value = layer
    }

    /**
     * Simulates live movement for other expedition peers along realistic trail tangents.
     */
    fun simulateTeammateMovement() {
        viewModelScope.launch {
            val currentList = _teamMembers.value.toMutableList()
            for (i in currentList.indices) {
                val m = currentList[i]
                if (m.id != userId) {
                    val latShift = ((-2..2).random() * 0.0003)
                    val lngShift = ((-2..2).random() * 0.0003)
                    val newLoc = m.location.copy(
                        latitude = m.location.latitude + latShift,
                        longitude = m.location.longitude + lngShift,
                        altitude = m.location.altitude + (-1..2).random(),
                        speed = (0.8f + (0..10).random() * 0.1f),
                        bearing = (0..359).random().toFloat(),
                        timestamp = System.currentTimeMillis()
                    )
                    val packet = TelemetryPacket(
                        type = PacketType.LOCATION_UPDATE,
                        tripCode = activeTrip.value?.code ?: "TREK88",
                        senderId = m.id,
                        senderName = m.name,
                        callSign = m.callSign,
                        colorHex = m.colorHex,
                        latitude = newLoc.latitude,
                        longitude = newLoc.longitude,
                        altitude = newLoc.altitude,
                        bearing = newLoc.bearing,
                        speed = newLoc.speed,
                        accuracy = newLoc.accuracy,
                        batteryPct = (m.batteryPct - (0..1).random()).coerceAtLeast(10),
                        isCharging = false,
                        status = MemberStatus.ACTIVE,
                        timestamp = System.currentTimeMillis()
                    )
                    app.networkManager.simulatePeerMovement(packet)
                }
            }
        }
    }

    class Factory(private val app: TrekSyncApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrekViewModel(app, app.repository) as T
        }
    }
}
