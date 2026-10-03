package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

class LocationEngine(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Dynamic polling interval in milliseconds
    private var currentIntervalMs: Long = 4000L
    private var minDistanceMeters: Float = 2.0f

    /**
     * Streams high-accuracy GPS coordinates with dynamic adaptive battery rate.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<UserLocation> = callbackFlow {
        var lastLocation: Location? = null

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    val userLoc = convertLocation(loc, lastLocation)
                    lastLocation = loc
                    trySend(userLoc)
                }
            }
        }

        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMs)
                .setMinUpdateIntervalMillis(2000L)
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnFailureListener {
                    // Fallback to legacy LocationManager if Google Play Services fails
                    startLegacyLocationListener(locationManager) { loc ->
                        val userLoc = convertLocation(loc, lastLocation)
                        lastLocation = loc
                        trySend(userLoc)
                    }
                }
        } catch (e: SecurityException) {
            // Permission not granted or exception handled
        } catch (e: Exception) {
            // GPS fallback
            startLegacyLocationListener(locationManager) { loc ->
                val userLoc = convertLocation(loc, lastLocation)
                lastLocation = loc
                trySend(userLoc)
            }
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(locationCallback)
            } catch (ignored: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLegacyLocationListener(
        manager: LocationManager,
        onLocation: (Location) -> Unit
    ) {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocation(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                currentIntervalMs,
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
        } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                currentIntervalMs,
                minDistanceMeters,
                listener,
                Looper.getMainLooper()
            )
        }
    }

    private fun convertLocation(location: Location, last: Location?): UserLocation {
        // Calculate speed or bearing if not natively provided by sensor
        var calculatedSpeed = location.speed
        var calculatedBearing = location.bearing

        if (last != null && location.time > last.time) {
            val deltaSec = (location.time - last.time) / 1000.0f
            if (deltaSec > 0 && calculatedSpeed == 0f) {
                calculatedSpeed = location.distanceTo(last) / deltaSec
            }
            if (calculatedBearing == 0f && location.distanceTo(last) > 3.0f) {
                calculatedBearing = last.bearingTo(location)
                if (calculatedBearing < 0) calculatedBearing += 360f
            }
        }

        return UserLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            bearing = calculatedBearing,
            speed = calculatedSpeed,
            accuracy = location.accuracy,
            timestamp = location.time
        )
    }

    /**
     * Adjusts polling frequency dynamically based on user motion to optimize battery consumption.
     */
    fun updateAdaptiveFrequency(isMoving: Boolean, speedMps: Float) {
        currentIntervalMs = when {
            speedMps > 4.0f -> 2500L // Running / Cycling / Vehicle
            speedMps > 0.8f -> 5000L // Active trekking / walking
            isMoving -> 8000L        // Slow movement
            else -> 20000L           // Stationary / at camp
        }
        minDistanceMeters = if (speedMps > 1.5f) 3.0f else 1.0f
    }
}
