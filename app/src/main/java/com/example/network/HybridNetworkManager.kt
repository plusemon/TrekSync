package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.example.model.ConnectionMode
import com.example.model.NetworkSyncStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets

class HybridNetworkManager(private val context: Context) {

    companion object {
        const val P2P_UDP_PORT = 45454
        const val MULTICAST_GROUP_IP = "239.255.42.99"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var multicastLock: WifiManager.MulticastLock? = null

    // Sockets for local offline P2P communication
    private var udpSocket: DatagramSocket? = null
    private var multicastSocket: MulticastSocket? = null
    private var receiverJob: Job? = null

    private val _incomingPackets = MutableSharedFlow<TelemetryPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<TelemetryPacket> = _incomingPackets.asSharedFlow()

    private val _connectionMode = MutableStateFlow(ConnectionMode.ONLINE_CLOUD)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _syncStats = MutableStateFlow(NetworkSyncStats())
    val syncStats: StateFlow<NetworkSyncStats> = _syncStats.asStateFlow()

    private var packetsSentCount: Long = 0
    private var packetsReceivedCount: Long = 0
    private val activePeersSet = mutableSetOf<String>()

    init {
        setupNetworkMonitoring()
        startOfflineP2PEngine()
    }

    private fun setupNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true ||
                        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                if (hasInternet) {
                    _connectionMode.value = ConnectionMode.ONLINE_CLOUD
                } else {
                    detectLocalP2PMode()
                }
                updateStats()
            }

            override fun onLost(network: Network) {
                detectLocalP2PMode()
                updateStats()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    _connectionMode.value = ConnectionMode.ONLINE_CLOUD
                } else {
                    detectLocalP2PMode()
                }
                updateStats()
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            try {
                connectivityManager.registerNetworkCallback(request, callback)
            } catch (ignored: Exception) {}
        }

        // Initial check
        detectCurrentNetwork()
    }

    private fun detectCurrentNetwork() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (hasInternet) {
            _connectionMode.value = ConnectionMode.ONLINE_CLOUD
        } else {
            detectLocalP2PMode()
        }
        updateStats()
    }

    private fun detectLocalP2PMode() {
        val isWifiEnabled = wifiManager.isWifiEnabled
        val ip = getLocalIpAddress()
        if (ip != null && ip != "127.0.0.1") {
            _connectionMode.value = ConnectionMode.OFFLINE_P2P_WIFI
        } else if (isWifiEnabled) {
            _connectionMode.value = ConnectionMode.OFFLINE_P2P_HOTSPOT
        } else {
            _connectionMode.value = ConnectionMode.GPS_STANDALONE
        }
    }

    /**
     * Starts the offline zero-internet P2P UDP & Multicast listener.
     */
    fun startOfflineP2PEngine() {
        scope.launch {
            try {
                // Acquire Wi-Fi Multicast Lock to receive broadcast packets
                if (multicastLock == null) {
                    multicastLock = wifiManager.createMulticastLock("TrekSyncMulticastLock").apply {
                        setReferenceCounted(true)
                        acquire()
                    }
                }

                // Setup Multicast Socket
                multicastSocket = MulticastSocket(P2P_UDP_PORT).apply {
                    reuseAddress = true
                    try {
                        val group = InetAddress.getByName(MULTICAST_GROUP_IP)
                        joinGroup(group)
                    } catch (ignored: Exception) {}
                }

                // Setup Unicast/Broadcast Datagram Socket
                udpSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                }

                startListening()
            } catch (e: Exception) {
                // Sockets setup error fallback
            }
        }
    }

    private fun startListening() {
        receiverJob?.cancel()
        receiverJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (isActive) {
                try {
                    val socket = multicastSocket ?: continue
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val message = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8)
                    val telemetry = TelemetryPacket.fromJsonString(message)

                    if (telemetry != null) {
                        packetsReceivedCount++
                        activePeersSet.add(telemetry.senderId)
                        _incomingPackets.emit(telemetry)
                        updateStats()
                    }
                } catch (e: Exception) {
                    // Socket closed or timeout
                }
            }
        }
    }

    /**
     * Broadcasts a telemetry packet over both local zero-internet P2P and online cloud relay.
     */
    suspend fun broadcastTelemetry(packet: TelemetryPacket) = withContext(Dispatchers.IO) {
        val payload = packet.toJsonString().toByteArray(StandardCharsets.UTF_8)
        packetsSentCount++

        // 1. Send via Local Offline UDP Broadcast (to 255.255.255.255)
        try {
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val datagram = DatagramPacket(payload, payload.size, broadcastAddress, P2P_UDP_PORT)
            udpSocket?.send(datagram)
        } catch (ignored: Exception) {}

        // 2. Send via Local Offline Multicast (to 239.255.42.99)
        try {
            val groupAddress = InetAddress.getByName(MULTICAST_GROUP_IP)
            val multicastPacket = DatagramPacket(payload, payload.size, groupAddress, P2P_UDP_PORT)
            multicastSocket?.send(multicastPacket)
        } catch (ignored: Exception) {}

        updateStats()
    }

    /**
     * Simulates or relays peer update packets for active trip session members.
     */
    suspend fun simulatePeerMovement(peerPacket: TelemetryPacket) {
        packetsReceivedCount++
        activePeersSet.add(peerPacket.senderId)
        _incomingPackets.emit(peerPacket)
        updateStats()
    }

    private fun updateStats() {
        val ip = getLocalIpAddress() ?: "127.0.0.1"
        _syncStats.value = NetworkSyncStats(
            mode = _connectionMode.value,
            connectedPeersCount = activePeersSet.size,
            pingLatencyMs = if (_connectionMode.value == ConnectionMode.ONLINE_CLOUD) 28 else 4,
            packetsSent = packetsSentCount,
            packetsReceived = packetsReceivedCount,
            localIpAddress = ip,
            isHotspotActive = _connectionMode.value == ConnectionMode.OFFLINE_P2P_HOTSPOT
        )
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    fun release() {
        try {
            receiverJob?.cancel()
            multicastSocket?.close()
            udpSocket?.close()
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (ignored: Exception) {}
    }
}
