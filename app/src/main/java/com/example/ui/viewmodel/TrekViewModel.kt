package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TrekSyncApplication
import com.example.data.local.TrekRepository
import com.example.location.BatteryInfo
import com.example.model.ConnectionMode
import com.example.model.GpxPoint
import com.example.model.GpxRoute
import com.example.model.HeartbeatState
import com.example.model.MapLayerType
import com.example.model.MapProviderEngine
import com.example.model.MemberStatus
import com.example.model.NetworkSyncStats
import com.example.model.OffTrailDeviation
import com.example.model.SosAlert
import com.example.model.TeamMember
import com.example.model.TripSession
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import com.example.network.PacketType
import com.example.network.TelemetryPacket
import com.example.service.TrackingForegroundService
import com.example.util.GpxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
            altitude = 290.0,
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

    // Map Layer & Engine
    private val _mapProviderEngine = MutableStateFlow(
        if (com.example.BuildConfig.MAPS_API_KEY.isNotEmpty() && com.example.BuildConfig.MAPS_API_KEY != "DEFAULT_MAPS_KEY") {
            MapProviderEngine.GOOGLE_MAPS
        } else {
            MapProviderEngine.OPEN_TOPO_OFFLINE
        }
    )
    val mapProviderEngine: StateFlow<MapProviderEngine> = _mapProviderEngine.asStateFlow()

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

    // -------------------------------------------------------------
    // GPX Route & Off-Trail Deviation Engine
    // -------------------------------------------------------------
    private val _loadedGpxRoute = MutableStateFlow<GpxRoute?>(null)
    val loadedGpxRoute: StateFlow<GpxRoute?> = _loadedGpxRoute.asStateFlow()

    private val _offTrailDeviation = MutableStateFlow(OffTrailDeviation())
    val offTrailDeviation: StateFlow<OffTrailDeviation> = _offTrailDeviation.asStateFlow()

    var offTrailThresholdMeters: Double = 50.0

    private val _gpxImportMessage = MutableStateFlow<String?>(null)
    val gpxImportMessage: StateFlow<String?> = _gpxImportMessage.asStateFlow()

    // -------------------------------------------------------------
    // Peer Heartbeat & Lost Contact Watchdog
    // -------------------------------------------------------------
    private val _lostContactMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val lostContactMembers: StateFlow<List<TeamMember>> = _lostContactMembers.asStateFlow()

    private val notifiedLostMemberIds = mutableSetOf<String>()

    init {
        initDefaultTrip()
        startSensorCollection()
        listenToIncomingPackets()
        startHeartbeatWatchdog()
        loadSampleGpxRoute()
    }

    private fun initDefaultTrip() {
        viewModelScope.launch {
            val defaultTrip = TripSession(
                id = "TRIP_ALPINE_01",
                name = "Pacific Crest Expedition",
                code = "TREK88",
                createdAt = System.currentTimeMillis(),
                leaderId = userId,
                description = "High altitude mountain trail real-time group sync with offline GPX guidance",
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
        val now = System.currentTimeMillis()
        val members = listOf(
            TeamMember(
                id = "USER_LEADER",
                name = userName,
                callSign = userCallSign,
                colorHex = "#00E676",
                location = center,
                batteryPct = 94,
                isCharging = false,
                lastSeenTimestamp = now,
                connectionMode = ConnectionMode.ONLINE_CLOUD,
                isLeader = true,
                status = MemberStatus.ACTIVE,
                heartbeatState = HeartbeatState.ACTIVE,
                secondsSinceLastSeen = 0L
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
                lastSeenTimestamp = now - 5000L, // 5s ago: Active
                connectionMode = ConnectionMode.OFFLINE_P2P_HOTSPOT,
                isLeader = false,
                status = MemberStatus.ACTIVE,
                heartbeatState = HeartbeatState.ACTIVE,
                secondsSinceLastSeen = 5L
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
                lastSeenTimestamp = now - 38000L, // 38s ago: Stale
                connectionMode = ConnectionMode.OFFLINE_P2P_WIFI,
                isLeader = false,
                status = MemberStatus.ACTIVE,
                heartbeatState = HeartbeatState.STALE,
                secondsSinceLastSeen = 38L
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
                    speed = 0.0f,
                    accuracy = 4f
                ),
                batteryPct = 42,
                isCharging = false,
                lastSeenTimestamp = now - 72000L, // 72s ago: Lost Contact
                connectionMode = ConnectionMode.GPS_STANDALONE,
                isLeader = false,
                status = MemberStatus.ACTIVE,
                heartbeatState = HeartbeatState.LOST_CONTACT,
                secondsSinceLastSeen = 72L
            )
        )
        _teamMembers.value = members
        updateLostContactList(members)
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
                recalculateOffTrailDeviation(loc, _loadedGpxRoute.value)
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

    // -------------------------------------------------------------
    // Heartbeat Watchdog Implementation (Runs Every 5s)
    // -------------------------------------------------------------
    private fun startHeartbeatWatchdog() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000L)
                val now = System.currentTimeMillis()
                val currentMembers = _teamMembers.value
                var hasChanges = false
                val updatedList = currentMembers.map { member ->
                    if (member.id == userId) {
                        member.copy(lastSeenTimestamp = now, heartbeatState = HeartbeatState.ACTIVE, secondsSinceLastSeen = 0L)
                    } else {
                        val elapsedSec = ((now - member.lastSeenTimestamp) / 1000L).coerceAtLeast(0L)
                        val newState = when {
                            elapsedSec < 30L -> HeartbeatState.ACTIVE
                            elapsedSec <= 60L -> HeartbeatState.STALE
                            else -> HeartbeatState.LOST_CONTACT
                        }

                        if (newState != member.heartbeatState || elapsedSec != member.secondsSinceLastSeen) {
                            hasChanges = true
                            member.copy(
                                heartbeatState = newState,
                                secondsSinceLastSeen = elapsedSec,
                                status = if (newState == HeartbeatState.LOST_CONTACT) MemberStatus.DISCONNECTED else member.status
                            )
                        } else {
                            member
                        }
                    }
                }

                if (hasChanges) {
                    _teamMembers.value = updatedList
                    updateLostContactList(updatedList)
                }
            }
        }
    }

    private fun updateLostContactList(members: List<TeamMember>) {
        val lost = members.filter { it.id != userId && it.heartbeatState == HeartbeatState.LOST_CONTACT }
        _lostContactMembers.value = lost

        // Trigger alert for newly lost members
        for (m in lost) {
            if (!notifiedLostMemberIds.contains(m.id)) {
                notifiedLostMemberIds.add(m.id)
                triggerLostContactAlert(m)
            }
        }

        // Clean up resolved notifications
        val lostIds = lost.map { it.id }.toSet()
        notifiedLostMemberIds.retainAll(lostIds)
    }

    private fun triggerLostContactAlert(member: TeamMember) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 400, 200, 400),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {
            // Safe fallback if vibrate permission is constrained in test environment
        }
    }

    fun dismissLostContactAlert(memberId: String) {
        _lostContactMembers.value = _lostContactMembers.value.filterNot { it.id == memberId }
    }

    // -------------------------------------------------------------
    // GPX Loading & Off-Trail Computation
    // -------------------------------------------------------------
    fun loadGpxFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = GpxParser.parseUri(context, uri)
            result.onSuccess { route ->
                _loadedGpxRoute.value = route
                _gpxImportMessage.value = "Loaded: ${route.name} (${String.format("%.1f", route.totalDistanceMeters / 1000.0)} km, ${route.points.size} pts)"
                if (route.waypoints.isNotEmpty()) {
                    _waypoints.value = _waypoints.value + route.waypoints
                }
                recalculateOffTrailDeviation(_currentLocation.value, route)
            }.onFailure { err ->
                _gpxImportMessage.value = "GPX Import Error: ${err.message ?: "Invalid file"}"
            }
        }
    }

    fun loadGpxString(xmlContent: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val result = GpxParser.parseString(xmlContent)
            result.onSuccess { route ->
                _loadedGpxRoute.value = route
                _gpxImportMessage.value = "Loaded trail: ${route.name}"
                if (route.waypoints.isNotEmpty()) {
                    _waypoints.value = _waypoints.value + route.waypoints
                }
                recalculateOffTrailDeviation(_currentLocation.value, route)
            }.onFailure { err ->
                _gpxImportMessage.value = "GPX Parse Error: ${err.message}"
            }
        }
    }

    fun loadSampleGpxRoute() {
        loadGpxString(GpxParser.SAMPLE_ALPINE_CREST_GPX)
    }

    fun clearLoadedGpx() {
        _loadedGpxRoute.value = null
        _offTrailDeviation.value = OffTrailDeviation()
        _gpxImportMessage.value = "GPX Route cleared"
    }

    fun clearGpxMessage() {
        _gpxImportMessage.value = null
    }

    /**
     * Calculates the perpendicular / cross-track deviation between current GPS location
     * and the nearest segment on the loaded GPX polyline.
     */
    private fun recalculateOffTrailDeviation(currentLoc: UserLocation, route: GpxRoute?) {
        if (route == null || route.points.size < 2) {
            _offTrailDeviation.value = OffTrailDeviation()
            return
        }

        val points = route.points
        var minDistanceMeters = Double.MAX_VALUE
        var closestPoint: GpxPoint? = null
        var closestSegmentIndex = 0

        val userLat = currentLoc.latitude
        val userLon = currentLoc.longitude
        val cosLat = cos(Math.toRadians(userLat))

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            // Convert to local metric coordinates
            val x1 = Math.toRadians(p1.longitude - userLon) * 6371000.0 * cosLat
            val y1 = Math.toRadians(p1.latitude - userLat) * 6371000.0
            val x2 = Math.toRadians(p2.longitude - userLon) * 6371000.0 * cosLat
            val y2 = Math.toRadians(p2.latitude - userLat) * 6371000.0

            val dx = x2 - x1
            val dy = y2 - y1
            val segLenSq = dx * dx + dy * dy

            val t = if (segLenSq > 0.0) {
                ((-x1 * dx - y1 * dy) / segLenSq).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            val projX = x1 + t * dx
            val projY = y1 + t * dy
            val dist = sqrt(projX * projX + projY * projY)

            if (dist < minDistanceMeters) {
                minDistanceMeters = dist
                closestSegmentIndex = i
                val projLat = p1.latitude + t * (p2.latitude - p1.latitude)
                val projLon = p1.longitude + t * (p2.longitude - p1.longitude)
                val projEle = p1.elevationMeters + t * (p2.elevationMeters - p1.elevationMeters)
                closestPoint = GpxPoint(projLat, projLon, projEle)
            }
        }

        if (closestPoint != null) {
            // Bearing from trail nearest point to user location (indicates where the user strayed)
            val bearingDeg = calculateBearing(
                closestPoint.latitude, closestPoint.longitude,
                userLat, userLon
            )
            val cardinal = cardinalFromBearing(bearingDeg)

            // Compute remaining trail distance from closest segment to trail finish
            var remainingDist = 0.0
            var remainingGain = 0.0
            remainingDist += GpxParser.haversineDistanceMeters(
                closestPoint.latitude, closestPoint.longitude,
                points[closestSegmentIndex + 1].latitude, points[closestSegmentIndex + 1].longitude
            )

            for (k in (closestSegmentIndex + 1) until points.size - 1) {
                val pA = points[k]
                val pB = points[k + 1]
                remainingDist += GpxParser.haversineDistanceMeters(pA.latitude, pA.longitude, pB.latitude, pB.longitude)
                if (pB.elevationMeters > pA.elevationMeters) {
                    remainingGain += (pB.elevationMeters - pA.elevationMeters)
                }
            }

            // Sanity bounds: if distance > 10km (10000m), user hasn't arrived at the trailhead yet
            val isFarFromTrailhead = minDistanceMeters > 10000.0
            val isOff = minDistanceMeters > offTrailThresholdMeters && !isFarFromTrailhead

            _offTrailDeviation.value = OffTrailDeviation(
                isOffTrail = isOff,
                distanceMeters = minDistanceMeters,
                nearestPoint = closestPoint,
                directionAngleDeg = bearingDeg.toFloat(),
                cardinalDirection = cardinal,
                remainingTrailDistanceMeters = remainingDist,
                elevationGainRemainingMeters = remainingGain
            )
        }
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

    private fun cardinalFromBearing(deg: Double): String {
        return when (deg.toInt()) {
            in 0..22, in 338..360 -> "North"
            in 23..67 -> "Northeast"
            in 68..112 -> "East"
            in 113..157 -> "Southeast"
            in 158..202 -> "South"
            in 203..247 -> "Southwest"
            in 248..292 -> "West"
            else -> "Northwest"
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
            isSosActive = _isSosActive.value,
            heartbeatState = HeartbeatState.ACTIVE,
            secondsSinceLastSeen = 0L
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
                breadcrumbTrail = if (newTrail.size > 20) newTrail.takeLast(20) else newTrail,
                heartbeatState = HeartbeatState.ACTIVE,
                secondsSinceLastSeen = 0L
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
                breadcrumbTrail = listOf(memberLoc),
                heartbeatState = HeartbeatState.ACTIVE,
                secondsSinceLastSeen = 0L
            )
        }

        if (idx >= 0) {
            currentList[idx] = updated
        } else {
            currentList.add(updated)
        }
        _teamMembers.value = currentList
        updateLostContactList(currentList)
    }

    fun startForegroundTracking(context: Context) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val trip = activeTrip.value ?: return
        try {
            TrackingForegroundService.startService(
                context = context,
                tripId = trip.id,
                tripCode = trip.code,
                userId = userId,
                userName = userName,
                callSign = userCallSign
            )
            _isTrackingActive.value = true
        } catch (_: Exception) {}
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

    fun switchMapProvider(engine: MapProviderEngine) {
        _mapProviderEngine.value = engine
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
