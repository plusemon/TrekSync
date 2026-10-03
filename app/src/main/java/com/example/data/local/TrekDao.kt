package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveTrip(): Flow<TripEntity?>

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE code = :code LIMIT 1")
    suspend fun getTripByCode(code: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET isActive = 0")
    suspend fun deactivateAllTrips()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name ASC")
    fun getMembersForTrip(tripId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE tripId = :tripId")
    suspend fun getMembersList(tripId: String): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE tripId = :tripId AND id = :memberId")
    suspend fun deleteMember(tripId: String, memberId: String)
}

@Dao
interface BreadcrumbDao {
    @Query("SELECT * FROM breadcrumbs WHERE tripId = :tripId AND memberId = :memberId ORDER BY timestamp ASC")
    fun getBreadcrumbsForMember(tripId: String, memberId: String): Flow<List<BreadcrumbEntity>>

    @Query("SELECT * FROM breadcrumbs WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getAllBreadcrumbsForTrip(tripId: String): Flow<List<BreadcrumbEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreadcrumb(breadcrumb: BreadcrumbEntity)

    @Query("DELETE FROM breadcrumbs WHERE tripId = :tripId")
    suspend fun deleteBreadcrumbsForTrip(tripId: String)
}

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getWaypointsForTrip(tripId: String): Flow<List<WaypointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteWaypoint(id: String)
}

@Dao
interface SosAlertDao {
    @Query("SELECT * FROM sos_alerts WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getSosAlertsForTrip(tripId: String): Flow<List<SosAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SosAlertEntity)

    @Query("UPDATE sos_alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun resolveAlert(alertId: String)
}
