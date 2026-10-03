package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.TrekDatabase
import com.example.data.local.TrekRepository
import com.example.location.BatteryTelemetryManager
import com.example.location.CompassSensorEngine
import com.example.location.LocationEngine
import com.example.network.HybridNetworkManager

class TrekSyncApplication : Application() {

    companion object {
        const val TRACKING_CHANNEL_ID = "treksync_tracking_channel"
        const val SOS_CHANNEL_ID = "treksync_sos_channel"

        lateinit var instance: TrekSyncApplication
            private set
    }

    lateinit var database: TrekDatabase
        private set
    lateinit var repository: TrekRepository
        private set
    lateinit var networkManager: HybridNetworkManager
        private set
    lateinit var locationEngine: LocationEngine
        private set
    lateinit var compassEngine: CompassSensorEngine
        private set
    lateinit var batteryManager: BatteryTelemetryManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        database = TrekDatabase.getInstance(this)
        repository = TrekRepository(database)
        networkManager = HybridNetworkManager(this)
        locationEngine = LocationEngine(this)
        compassEngine = CompassSensorEngine(this)
        batteryManager = BatteryTelemetryManager(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                getString(R.string.notification_channel_tracking_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_tracking_desc)
                setShowBadge(false)
            }

            val sosChannel = NotificationChannel(
                SOS_CHANNEL_ID,
                getString(R.string.notification_channel_sos_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_sos_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(trackingChannel)
            manager.createNotificationChannel(sosChannel)
        }
    }
}
