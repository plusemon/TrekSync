package com.example

import com.example.model.MemberStatus
import com.example.model.Waypoint
import com.example.model.WaypointType
import com.example.network.PacketType
import com.example.network.TelemetryPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PacketProtocolTest {

    @Test
    fun testLocationPacketSerializationAndParsing() {
        val original = TelemetryPacket(
            type = PacketType.LOCATION_UPDATE,
            tripCode = "TREK99",
            senderId = "MEMBER_01",
            senderName = "Alex",
            callSign = "LEAD-1",
            colorHex = "#00E676",
            latitude = 45.123456,
            longitude = -121.654321,
            altitude = 1850.5,
            bearing = 120.0f,
            speed = 2.4f,
            accuracy = 3.5f,
            batteryPct = 85,
            isCharging = false,
            status = MemberStatus.ACTIVE,
            timestamp = 1700000000000L
        )

        val jsonStr = original.toJsonString()
        val parsed = TelemetryPacket.fromJsonString(jsonStr)

        assertNotNull(parsed)
        assertEquals(PacketType.LOCATION_UPDATE, parsed?.type)
        assertEquals("TREK99", parsed?.tripCode)
        assertEquals("MEMBER_01", parsed?.senderId)
        assertEquals(45.123456, parsed!!.latitude, 0.00001)
        assertEquals(-121.654321, parsed.longitude, 0.00001)
        assertEquals(1850.5, parsed.altitude, 0.1)
        assertEquals(85, parsed.batteryPct)
    }

    @Test
    fun testWaypointPacketSerialization() {
        val wp = Waypoint(
            id = "WP_100",
            tripId = "TREK99",
            name = "Water Spring",
            type = WaypointType.WATER_SOURCE,
            latitude = 45.13,
            longitude = -121.64,
            altitude = 1600.0,
            note = "Clear spring",
            createdBy = "Alex"
        )

        val packet = TelemetryPacket(
            type = PacketType.WAYPOINT_SHARED,
            tripCode = "TREK99",
            senderId = "MEMBER_01",
            senderName = "Alex",
            callSign = "LEAD-1",
            colorHex = "#00E676",
            latitude = 45.13,
            longitude = -121.64,
            altitude = 1600.0,
            bearing = 0f,
            speed = 0f,
            accuracy = 1f,
            batteryPct = 85,
            isCharging = false,
            status = MemberStatus.ACTIVE,
            timestamp = 1700000000000L,
            waypointData = wp
        )

        val jsonStr = packet.toJsonString()
        val parsed = TelemetryPacket.fromJsonString(jsonStr)

        assertNotNull(parsed)
        assertNotNull(parsed?.waypointData)
        assertEquals("Water Spring", parsed?.waypointData?.name)
        assertEquals(WaypointType.WATER_SOURCE, parsed?.waypointData?.type)
    }
}
