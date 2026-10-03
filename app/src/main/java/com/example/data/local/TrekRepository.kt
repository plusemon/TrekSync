package com.example.data.local

import com.example.model.ConnectionMode
import com.example.model.MemberStatus
import com.example.model.SosAlert
import com.example.model.TeamMember
import com.example.model.TripSession
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrekRepository(private val db: TrekDatabase) {

    private val tripDao = db.tripDao()
    private val memberDao = db.memberDao()
    private val breadcrumbDao = db.breadcrumbDao()
    private val waypointDao = db.waypointDao()
    private val sosAlertDao = db.sosAlertDao()

    val activeTrip: Flow<TripSession?> = tripDao.getActiveTrip().map { entity ->
        entity?.let {
            TripSession(
                id = it.id,
                name = it.name,
                code = it.code,
                createdAt = it.createdAt,
                leaderId = it.leaderId,
                description = it.description,
                geofenceRadiusMeters = it.geofenceRadiusMeters,
                isActive = it.isActive,
                syncMode = try { ConnectionMode.valueOf(it.syncMode) } catch (e: Exception) { ConnectionMode.ONLINE_CLOUD },
                totalDistanceHikedMeters = it.totalDistanceMeters,
                elevationGainMeters = it.elevationGainMeters,
                maxSpeedKmh = it.maxSpeedKmh
            )
        }
    }

    val allTrips: Flow<List<TripSession>> = tripDao.getAllTrips().map { list ->
        list.map { entity ->
            TripSession(
                id = entity.id,
                name = entity.name,
                code = entity.code,
                createdAt = entity.createdAt,
                leaderId = entity.leaderId,
                description = entity.description,
                geofenceRadiusMeters = entity.geofenceRadiusMeters,
                isActive = entity.isActive,
                syncMode = try { ConnectionMode.valueOf(entity.syncMode) } catch (e: Exception) { ConnectionMode.ONLINE_CLOUD },
                totalDistanceHikedMeters = entity.totalDistanceMeters,
                elevationGainMeters = entity.elevationGainMeters,
                maxSpeedKmh = entity.maxSpeedKmh
            )
        }
    }

    fun getMembersForTrip(tripId: String): Flow<List<TeamMember>> {
        return memberDao.getMembersForTrip(tripId).map { list ->
            list.map { entity ->
                TeamMember(
                    id = entity.id,
                    name = entity.name,
                    callSign = entity.callSign,
                    colorHex = entity.colorHex,
                    location = UserLocation(
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        altitude = entity.altitude,
                        bearing = entity.bearing,
                        speed = entity.speed,
                        accuracy = entity.accuracy,
                        timestamp = entity.lastSeenTimestamp
                    ),
                    batteryPct = entity.batteryPct,
                    isCharging = entity.isCharging,
                    lastSeenTimestamp = entity.lastSeenTimestamp,
                    connectionMode = try { ConnectionMode.valueOf(entity.connectionMode) } catch (e: Exception) { ConnectionMode.ONLINE_CLOUD },
                    isLeader = entity.isLeader,
                    status = try { MemberStatus.valueOf(entity.status) } catch (e: Exception) { MemberStatus.ACTIVE },
                    isSosActive = entity.isSosActive,
                    sosMessage = entity.sosMessage
                )
            }
        }
    }

    fun getWaypointsForTrip(tripId: String): Flow<List<Waypoint>> {
        return waypointDao.getWaypointsForTrip(tripId).map { list ->
            list.map { entity ->
                Waypoint(
                    id = entity.id,
                    tripId = entity.tripId,
                    name = entity.name,
                    type = try { WaypointType.valueOf(entity.type) } catch (e: Exception) { WaypointType.CHECKPOINT },
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    altitude = entity.altitude,
                    note = entity.note,
                    createdBy = entity.createdBy,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    fun getSosAlertsForTrip(tripId: String): Flow<List<SosAlert>> {
        return sosAlertDao.getSosAlertsForTrip(tripId).map { list ->
            list.map { entity ->
                SosAlert(
                    id = entity.id,
                    senderId = entity.senderId,
                    senderName = entity.senderName,
                    senderCallSign = entity.senderCallSign,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    altitude = entity.altitude,
                    timestamp = entity.timestamp,
                    message = entity.message,
                    isResolved = entity.isResolved
                )
            }
        }
    }

    suspend fun createOrJoinTrip(trip: TripSession) = withContext(Dispatchers.IO) {
        tripDao.deactivateAllTrips()
        tripDao.insertTrip(
            TripEntity(
                id = trip.id,
                name = trip.name,
                code = trip.code,
                createdAt = trip.createdAt,
                leaderId = trip.leaderId,
                description = trip.description,
                geofenceRadiusMeters = trip.geofenceRadiusMeters,
                isActive = true,
                syncMode = trip.syncMode.name,
                totalDistanceMeters = trip.totalDistanceHikedMeters,
                elevationGainMeters = trip.elevationGainMeters,
                maxSpeedKmh = trip.maxSpeedKmh
            )
        )
    }

    suspend fun updateTripStats(tripId: String, distanceDelta: Double, elevationGainDelta: Double, maxSpeed: Float) = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId) ?: return@withContext
        val updated = trip.copy(
            totalDistanceMeters = trip.totalDistanceMeters + distanceDelta,
            elevationGainMeters = trip.elevationGainMeters + elevationGainDelta,
            maxSpeedKmh = maxOf(trip.maxSpeedKmh, maxSpeed)
        )
        tripDao.updateTrip(updated)
    }

    suspend fun upsertMember(tripId: String, member: TeamMember) = withContext(Dispatchers.IO) {
        memberDao.insertOrUpdateMember(
            MemberEntity(
                id = member.id,
                tripId = tripId,
                name = member.name,
                callSign = member.callSign,
                colorHex = member.colorHex,
                latitude = member.location.latitude,
                longitude = member.location.longitude,
                altitude = member.location.altitude,
                bearing = member.location.bearing,
                speed = member.location.speed,
                accuracy = member.location.accuracy,
                batteryPct = member.batteryPct,
                isCharging = member.isCharging,
                lastSeenTimestamp = member.lastSeenTimestamp,
                connectionMode = member.connectionMode.name,
                isLeader = member.isLeader,
                status = member.status.name,
                isSosActive = member.isSosActive,
                sosMessage = member.sosMessage
            )
        )
    }

    suspend fun addBreadcrumb(tripId: String, memberId: String, location: UserLocation) = withContext(Dispatchers.IO) {
        breadcrumbDao.insertBreadcrumb(
            BreadcrumbEntity(
                tripId = tripId,
                memberId = memberId,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                timestamp = location.timestamp
            )
        )
    }

    suspend fun addWaypoint(waypoint: Waypoint) = withContext(Dispatchers.IO) {
        waypointDao.insertWaypoint(
            WaypointEntity(
                id = waypoint.id,
                tripId = waypoint.tripId,
                name = waypoint.name,
                type = waypoint.type.name,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                altitude = waypoint.altitude,
                note = waypoint.note,
                createdBy = waypoint.createdBy,
                timestamp = waypoint.timestamp
            )
        )
    }

    suspend fun addSosAlert(tripId: String, alert: SosAlert) = withContext(Dispatchers.IO) {
        sosAlertDao.insertAlert(
            SosAlertEntity(
                id = alert.id,
                tripId = tripId,
                senderId = alert.senderId,
                senderName = alert.senderName,
                senderCallSign = alert.senderCallSign,
                latitude = alert.latitude,
                longitude = alert.longitude,
                altitude = alert.altitude,
                timestamp = alert.timestamp,
                message = alert.message,
                isResolved = alert.isResolved
            )
        )
    }

    suspend fun resolveSosAlert(alertId: String) = withContext(Dispatchers.IO) {
        sosAlertDao.resolveAlert(alertId)
    }
}
