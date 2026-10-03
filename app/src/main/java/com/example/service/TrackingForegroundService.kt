package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.TrekSyncApplication
import com.example.model.ConnectionMode
import com.example.model.MemberStatus
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.network.PacketType
import com.example.network.TelemetryPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {

    companion object {
        private const val TAG = "TrekSync:Service"

        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val ACTION_TRIGGER_SOS = "ACTION_TRIGGER_SOS"
        const val ACTION_CANCEL_SOS = "ACTION_CANCEL_SOS"

        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
        const val EXTRA_TRIP_CODE = "EXTRA_TRIP_CODE"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
        const val EXTRA_CALL_SIGN = "EXTRA_CALL_SIGN"

        const val NOTIFICATION_ID = 1001

        fun startService(
            context: Context,
            tripId: String,
            tripCode: String,
            userId: String,
            userName: String,
            callSign: String
        ) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRIP_ID, tripId)
                putExtra(EXTRA_TRIP_CODE, tripCode)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_CALL_SIGN, callSign)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var trackingJob: Job? = null

    // System Locks for Zero-Drop Background Execution
    private var wakeLock: PowerManager.WakeLock? = null

    private var tripId: String = "DEMO_TRIP"
    private var tripCode: String = "TREK01"
    private var userId: String = "USER_LEADER"
    private var userName: String = "Alex Rivera"
    private var callSign: String = "EAGLE-1"
    private var isSosActive: Boolean = false

    private var currentLocation: UserLocation = UserLocation(0.0, 0.0)
    private var currentBattery: Int = 100
    private var currentCharging: Boolean = false
    private var currentHeading: Float = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initWakeLock()
    }

    private fun initWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TrekSync:TrackingWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
            Log.d(TAG, "Initialized PARTIAL_WAKE_LOCK.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WakeLock", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24-hour safety timeout
                Log.i(TAG, "Acquired PARTIAL_WAKE_LOCK for background tracking.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.i(TAG, "Released PARTIAL_WAKE_LOCK.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: tripId
                tripCode = intent.getStringExtra(EXTRA_TRIP_CODE) ?: tripCode
                userId = intent.getStringExtra(EXTRA_USER_ID) ?: userId
                userName = intent.getStringExtra(EXTRA_USER_NAME) ?: userName
                callSign = intent.getStringExtra(EXTRA_CALL_SIGN) ?: callSign

                acquireWakeLock()
                val app = applicationContext as TrekSyncApplication
                app.networkManager.acquireMulticastLock()

                startForegroundWithNotification()
                startTrackingLoop()
            }
            ACTION_STOP -> {
                stopTrackingLoop()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TRIGGER_SOS -> {
                isSosActive = true
                broadcastSosBeacon()
                updateNotification()
            }
            ACTION_CANCEL_SOS -> {
                isSosActive = false
                updateNotification()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            statusText = "Tracking Active • Initializing GPS...",
            syncMode = ConnectionMode.ONLINE_CLOUD
        )

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        val app = applicationContext as TrekSyncApplication

        trackingJob = serviceScope.launch {
            combine(
                app.locationEngine.getLocationUpdates(),
                app.compassEngine.getHeadingFlow(),
                app.batteryManager.getBatteryInfoFlow(),
                app.networkManager.connectionMode
            ) { loc, heading, battery, mode ->
                DataSnapshot(loc, heading, battery.percentage, battery.isCharging, mode)
            }.collectLatest { data ->
                currentLocation = data.loc
                currentHeading = data.heading
                currentBattery = data.battery
                currentCharging = data.isCharging

                app.locationEngine.updateAdaptiveFrequency(
                    isMoving = data.loc.speed > 0.5f,
                    speedMps = data.loc.speed
                )

                app.repository.addBreadcrumb(tripId, userId, data.loc)

                val selfMember = TeamMember(
                    id = userId,
                    name = userName,
                    callSign = callSign,
                    colorHex = "#00E676",
                    location = data.loc.copy(bearing = data.heading),
                    batteryPct = data.battery,
                    isCharging = data.isCharging,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    connectionMode = data.mode,
                    isLeader = true,
                    status = if (isSosActive) MemberStatus.SOS_EMERGENCY else MemberStatus.ACTIVE,
                    isSosActive = isSosActive
                )
                app.repository.upsertMember(tripId, selfMember)

                val packet = TelemetryPacket(
                    type = if (isSosActive) PacketType.SOS_ALERT else PacketType.LOCATION_UPDATE,
                    tripCode = tripCode,
                    senderId = userId,
                    senderName = userName,
                    callSign = callSign,
                    colorHex = "#00E676",
                    latitude = data.loc.latitude,
                    longitude = data.loc.longitude,
                    altitude = data.loc.altitude,
                    bearing = data.heading,
                    speed = data.loc.speed,
                    accuracy = data.loc.accuracy,
                    batteryPct = data.battery,
                    isCharging = data.isCharging,
                    status = if (isSosActive) MemberStatus.SOS_EMERGENCY else MemberStatus.ACTIVE,
                    timestamp = System.currentTimeMillis(),
                    payloadMessage = if (isSosActive) "SOS DISTRESS SIGNAL ACTIVE" else null
                )
                app.networkManager.broadcastTelemetry(packet)

                val speedKmh = (data.loc.speed * 3.6f)
                val statusText = "Alt: ${data.loc.altitude.toInt()}m • Speed: %.1f km/h • Bat: %d%%"
                    .format(speedKmh, data.battery)
                updateNotification(statusText, data.mode)
            }
        }
    }

    private fun broadcastSosBeacon() {
        val app = applicationContext as TrekSyncApplication
        serviceScope.launch {
            val packet = TelemetryPacket(
                type = PacketType.SOS_ALERT,
                tripCode = tripCode,
                senderId = userId,
                senderName = userName,
                callSign = callSign,
                colorHex = "#FF3D00",
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                altitude = currentLocation.altitude,
                bearing = currentHeading,
                speed = currentLocation.speed,
                accuracy = currentLocation.accuracy,
                batteryPct = currentBattery,
                isCharging = currentCharging,
                status = MemberStatus.SOS_EMERGENCY,
                timestamp = System.currentTimeMillis(),
                payloadMessage = "CRITICAL: Member triggered Emergency Beacon!"
            )
            app.networkManager.broadcastTelemetry(packet)
        }
    }

    private fun updateNotification(
        statusText: String = "Tracking Group Expedition",
        syncMode: ConnectionMode = ConnectionMode.ONLINE_CLOUD
    ) {
        val notification = buildNotification(statusText, syncMode)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String, syncMode: ConnectionMode): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sosActionIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = if (isSosActive) ACTION_CANCEL_SOS else ACTION_TRIGGER_SOS
        }
        val sosPendingIntent = PendingIntent.getService(
            this,
            1,
            sosActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSosActive) "🚨 SOS ACTIVE • Trip: $tripCode" else "TrekSync Active • $tripCode"
        val modeBadge = "[${syncMode.label}]"

        return NotificationCompat.Builder(this, TrekSyncApplication.TRACKING_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$modeBadge $statusText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(if (isSosActive) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                if (isSosActive) "Cancel SOS" else "🚨 Trigger SOS",
                sosPendingIntent
            )
            .build()
    }

    private fun stopTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = null
    }

    override fun onDestroy() {
        stopTrackingLoop()
        releaseWakeLock()
        super.onDestroy()
    }

    private data class DataSnapshot(
        val loc: UserLocation,
        val heading: Float,
        val battery: Int,
        val isCharging: Boolean,
        val mode: ConnectionMode
    )
}
