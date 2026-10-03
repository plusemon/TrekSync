package com.example.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassSensorEngine(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /**
     * Flow of device azimuth (compass heading in degrees 0° to 360°).
     */
    fun getHeadingFlow(): Flow<Float> = callbackFlow {
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rMatrix = FloatArray(9)
        val iMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        var hasGravity = false
        var hasGeo = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    var azimuthInDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuthInDeg < 0) azimuthInDeg += 360f
                    trySend(azimuthInDeg)
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                    }

                    if (hasGravity && hasGeo) {
                        val success = SensorManager.getRotationMatrix(rMatrix, iMatrix, gravity, geomagnetic)
                        if (success) {
                            SensorManager.getOrientation(rMatrix, orientation)
                            var azimuthInDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            if (azimuthInDeg < 0) azimuthInDeg += 360f
                            trySend(azimuthInDeg)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magneticSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
