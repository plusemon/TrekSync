package com.example.network

import com.example.model.ConnectionMode
import com.example.model.MemberStatus
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.model.Waypoint
import com.example.model.WaypointType
import org.json.JSONObject

enum class PacketType {
    LOCATION_UPDATE,
    SOS_ALERT,
    WAYPOINT_SHARED,
    HEARTBEAT_PING,
    JOIN_ANNOUNCE
}

data class TelemetryPacket(
    val type: PacketType,
    val tripCode: String,
    val senderId: String,
    val senderName: String,
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
    val status: MemberStatus,
    val timestamp: Long,
    val payloadMessage: String? = null,
    val waypointData: Waypoint? = null
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("tripCode", tripCode)
        json.put("senderId", senderId)
        json.put("senderName", senderName)
        json.put("callSign", callSign)
        json.put("colorHex", colorHex)
        json.put("lat", latitude)
        json.put("lng", longitude)
        json.put("alt", altitude)
        json.put("bearing", bearing.toDouble())
        json.put("speed", speed.toDouble())
        json.put("accuracy", accuracy.toDouble())
        json.put("battery", batteryPct)
        json.put("isCharging", isCharging)
        json.put("status", status.name)
        json.put("ts", timestamp)
        payloadMessage?.let { json.put("msg", it) }

        waypointData?.let { wp ->
            val wpObj = JSONObject()
            wpObj.put("id", wp.id)
            wpObj.put("name", wp.name)
            wpObj.put("type", wp.type.name)
            wpObj.put("lat", wp.latitude)
            wpObj.put("lng", wp.longitude)
            wpObj.put("alt", wp.altitude)
            wpObj.put("note", wp.note)
            wpObj.put("createdBy", wp.createdBy)
            wpObj.put("ts", wp.timestamp)
            json.put("waypoint", wpObj)
        }

        return json.toString()
    }

    companion object {
        fun fromJsonString(str: String): TelemetryPacket? {
            return try {
                val json = JSONObject(str)
                val type = PacketType.valueOf(json.optString("type", PacketType.LOCATION_UPDATE.name))
                val tripCode = json.optString("tripCode", "")
                val senderId = json.optString("senderId", "")
                val senderName = json.optString("senderName", "Unknown")
                val callSign = json.optString("callSign", "MEMBER")
                val colorHex = json.optString("colorHex", "#00E676")
                val lat = json.optDouble("lat", 0.0)
                val lng = json.optDouble("lng", 0.0)
                val alt = json.optDouble("alt", 0.0)
                val bearing = json.optDouble("bearing", 0.0).toFloat()
                val speed = json.optDouble("speed", 0.0).toFloat()
                val accuracy = json.optDouble("accuracy", 0.0).toFloat()
                val battery = json.optInt("battery", 100)
                val isCharging = json.optBoolean("isCharging", false)
                val status = try {
                    MemberStatus.valueOf(json.optString("status", MemberStatus.ACTIVE.name))
                } catch (e: Exception) {
                    MemberStatus.ACTIVE
                }
                val ts = json.optLong("ts", System.currentTimeMillis())
                val msg = if (json.has("msg")) json.optString("msg") else null

                var wp: Waypoint? = null
                if (json.has("waypoint")) {
                    val wpObj = json.getJSONObject("waypoint")
                    wp = Waypoint(
                        id = wpObj.optString("id", ""),
                        tripId = tripCode,
                        name = wpObj.optString("name", "Waypoint"),
                        type = try { WaypointType.valueOf(wpObj.optString("type", WaypointType.CHECKPOINT.name)) } catch (e: Exception) { WaypointType.CHECKPOINT },
                        latitude = wpObj.optDouble("lat", 0.0),
                        longitude = wpObj.optDouble("lng", 0.0),
                        altitude = wpObj.optDouble("alt", 0.0),
                        note = wpObj.optString("note", ""),
                        createdBy = wpObj.optString("createdBy", senderName),
                        timestamp = wpObj.optLong("ts", System.currentTimeMillis())
                    )
                }

                TelemetryPacket(
                    type = type,
                    tripCode = tripCode,
                    senderId = senderId,
                    senderName = senderName,
                    callSign = callSign,
                    colorHex = colorHex,
                    latitude = lat,
                    longitude = lng,
                    altitude = alt,
                    bearing = bearing,
                    speed = speed,
                    accuracy = accuracy,
                    batteryPct = battery,
                    isCharging = isCharging,
                    status = status,
                    timestamp = ts,
                    payloadMessage = msg,
                    waypointData = wp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
