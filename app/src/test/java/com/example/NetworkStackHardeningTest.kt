package com.example

import com.example.model.MemberStatus
import com.example.network.PacketType
import com.example.network.TelemetryPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkStackHardeningTest {

    @Test
    fun testMathematicalSubnetBroadcastCalculation() {
        // Test subnet mask calculation for 192.168.43.1 / 24
        val ipBytes = byteArrayOf(192.toByte(), 168.toByte(), 43.toByte(), 1.toByte())
        val prefix = 24

        val ipInt = ((ipBytes[0].toInt() and 0xFF) shl 24) or
                ((ipBytes[1].toInt() and 0xFF) shl 16) or
                ((ipBytes[2].toInt() and 0xFF) shl 8) or
                (ipBytes[3].toInt() and 0xFF)

        val maskInt = (-1 shl (32 - prefix))
        val broadcastInt = ipInt or (maskInt.inv())

        val broadcastBytes = byteArrayOf(
            ((broadcastInt ushr 24) and 0xFF).toByte(),
            ((broadcastInt ushr 16) and 0xFF).toByte(),
            ((broadcastInt ushr 8) and 0xFF).toByte(),
            (broadcastInt and 0xFF).toByte()
        )

        val broadcastAddr = InetAddress.getByAddress(broadcastBytes)
        assertEquals("192.168.43.255", broadcastAddr.hostAddress)
    }

    @Test
    fun testSubnetBroadcastCalculationForSlash16() {
        val ipBytes = byteArrayOf(10.toByte(), 0.toByte(), 15.toByte(), 4.toByte())
        val prefix = 16

        val ipInt = ((ipBytes[0].toInt() and 0xFF) shl 24) or
                ((ipBytes[1].toInt() and 0xFF) shl 16) or
                ((ipBytes[2].toInt() and 0xFF) shl 8) or
                (ipBytes[3].toInt() and 0xFF)

        val maskInt = (-1 shl (32 - prefix))
        val broadcastInt = ipInt or (maskInt.inv())

        val broadcastBytes = byteArrayOf(
            ((broadcastInt ushr 24) and 0xFF).toByte(),
            ((broadcastInt ushr 16) and 0xFF).toByte(),
            ((broadcastInt ushr 8) and 0xFF).toByte(),
            (broadcastInt and 0xFF).toByte()
        )

        val broadcastAddr = InetAddress.getByAddress(broadcastBytes)
        assertEquals("10.0.255.255", broadcastAddr.hostAddress)
    }

    @Test
    fun testTelemetryPacketSosDistressPayload() {
        val packet = TelemetryPacket(
            type = PacketType.SOS_ALERT,
            tripCode = "EXPEDITION_1",
            senderId = "ALPHA_LEAD",
            senderName = "Alex Rivera",
            callSign = "EAGLE-1",
            colorHex = "#FF3D00",
            latitude = 47.6062,
            longitude = -122.3321,
            altitude = 1250.0,
            bearing = 180f,
            speed = 0f,
            accuracy = 2.0f,
            batteryPct = 42,
            isCharging = false,
            status = MemberStatus.SOS_EMERGENCY,
            timestamp = System.currentTimeMillis(),
            payloadMessage = "CRITICAL: Member triggered Emergency Beacon!"
        )

        val json = packet.toJsonString()
        val parsed = TelemetryPacket.fromJsonString(json)

        assertNotNull(parsed)
        assertEquals(PacketType.SOS_ALERT, parsed?.type)
        assertEquals(MemberStatus.SOS_EMERGENCY, parsed?.status)
        assertEquals("CRITICAL: Member triggered Emergency Beacon!", parsed?.payloadMessage)
        assertEquals(42, parsed?.batteryPct)
    }
}
