package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ConnectionMode
import com.example.model.MemberStatus
import com.example.model.WaypointType

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val createdAt: Long,
    val leaderId: String,
    val description: String,
    val geofenceRadiusMeters: Float,
    val isActive: Boolean,
    val syncMode: String,
    val totalDistanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val maxSpeedKmh: Float = 0f
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val callSign: String,
    val colorHex: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val bearing: Float,
    val speed: Float,
    val accuracy: Float,
    val batteryPct: Int,
    val isCharging: Boolean,
    val lastSeenTimestamp: Long,
    val connectionMode: String,
    val isLeader: Boolean,
    val status: String,
    val isSosActive: Boolean,
    val sosMessage: String?
)

@Entity(tableName = "breadcrumbs")
data class BreadcrumbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val memberId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val timestamp: Long
)

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val note: String,
    val createdBy: String,
    val timestamp: Long
)

@Entity(tableName = "sos_alerts")
data class SosAlertEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val senderId: String,
    val senderName: String,
    val senderCallSign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long,
    val message: String,
    val isResolved: Boolean
)
