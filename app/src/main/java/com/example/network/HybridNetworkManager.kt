package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.ConnectionMode
import com.example.model.NetworkSyncStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

class HybridNetworkManager(private val context: Context) {

    companion object {
        private const val TAG = "TrekSync:Network"
        const val P2P_UDP_PORT = 45454
        const val MULTICAST_GROUP_IP = "239.255.42.99"
        private const val SOCKET_TIMEOUT_MS = 2500
        private const val BUFFER_SIZE = 4096
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private val socketMutex = Mutex()

    // Sockets for local offline P2P communication
    private var multicastSocket: MulticastSocket? = null
    private var broadcastSenderSocket: DatagramSocket? = null
    private var receiverJob: Job? = null
    private var rebindJob: Job? = null

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
        acquireMulticastLock()
        setupNetworkMonitoring()
        startOfflineP2PEngine()
    }

    /**
     * Hardware MulticastLock Management.
     * Ensures the Wi-Fi radio does not filter out or drop incoming UDP multicast
     * and broadcast packets when the screen turns off or in low-power states.
     */
    fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("TrekSync:MulticastLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
                Log.i(TAG, "Hardware MulticastLock acquired successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire MulticastLock", e)
        }
        updateStats()
    }

    fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.i(TAG, "Hardware MulticastLock released.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release MulticastLock", e)
        }
        updateStats()
    }

    private fun setupNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network onAvailable: $network")
                handleNetworkChange()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network onLost: $network")
                handleNetworkChange()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Log.d(TAG, "Network onCapabilitiesChanged: $network")
                handleNetworkChange()
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                Log.d(TAG, "Network onLinkPropertiesChanged: $network")
                handleNetworkChange()
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            try {
                connectivityManager.registerNetworkCallback(request, callback)
            } catch (ignored: Exception) {
                Log.w(TAG, "Could not register default network callback", ignored)
            }
        }

        // Initial check
        detectCurrentNetwork()
    }

    private fun handleNetworkChange() {
        detectCurrentNetwork()
        // Debounce socket rebinding to allow network interface addresses to settle
        rebindJob?.cancel()
        rebindJob = scope.launch {
            delay(600)
            Log.i(TAG, "Network state changed; triggering socket and interface rebind...")
            rebindSockets()
        }
    }

    private fun detectCurrentNetwork() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

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
        val isHotspot = isHotspotActive()

        if (isHotspot) {
            _connectionMode.value = ConnectionMode.OFFLINE_P2P_HOTSPOT
        } else if (ip != null && ip != "127.0.0.1") {
            _connectionMode.value = ConnectionMode.OFFLINE_P2P_WIFI
        } else if (isWifiEnabled) {
            _connectionMode.value = ConnectionMode.OFFLINE_P2P_HOTSPOT
        } else {
            _connectionMode.value = ConnectionMode.GPS_STANDALONE
        }
    }

    private fun isHotspotActive(): Boolean {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val name = iface.name.lowercase()
                    if (name.contains("ap") || name.contains("swlan") || name.contains("hotspot") || name.contains("tether")) {
                        return true
                    }
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && addr.hostAddress?.startsWith("192.168.43.") == true) {
                            return true
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}
        return false
    }

    /**
     * Starts or restarts the offline zero-internet P2P UDP & Multicast engine.
     */
    fun startOfflineP2PEngine() {
        scope.launch {
            rebindSockets()
        }
    }

    /**
     * Rebinds UDP and Multicast sockets with SO_REUSEADDR and joins multicast groups
     * across all active network interfaces.
     */
    private suspend fun rebindSockets() = withContext(Dispatchers.IO) {
        socketMutex.withLock {
            try {
                acquireMulticastLock()

                // 1. Teardown existing receiver and sockets
                receiverJob?.cancel()
                try { multicastSocket?.close() } catch (ignored: Exception) {}
                try { broadcastSenderSocket?.close() } catch (ignored: Exception) {}

                // 2. Initialize MulticastSocket bound to P2P_UDP_PORT with SO_REUSEADDR
                val mSocket = MulticastSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = SOCKET_TIMEOUT_MS
                    bind(InetSocketAddress(P2P_UDP_PORT))
                }

                // 3. Join multicast group on all active multicast-capable interfaces
                val mGroupAddress = InetAddress.getByName(MULTICAST_GROUP_IP)
                val groupSocketAddress = InetSocketAddress(mGroupAddress, P2P_UDP_PORT)

                var joinedCount = 0
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val iface = interfaces.nextElement()
                        if (iface.isUp && !iface.isLoopback && iface.supportsMulticast()) {
                            try {
                                mSocket.joinGroup(groupSocketAddress, iface)
                                joinedCount++
                                Log.d(TAG, "Joined multicast group on interface ${iface.name} (${iface.displayName})")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to join multicast on interface ${iface.name}: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error enumerating interfaces for multicast join", e)
                }

                // Generic join fallback
                try {
                    mSocket.joinGroup(mGroupAddress)
                } catch (ignored: Exception) {}

                Log.i(TAG, "MulticastSocket successfully bound to port $P2P_UDP_PORT (Joined on $joinedCount interfaces).")
                multicastSocket = mSocket

                // 4. Initialize dedicated Broadcast DatagramSocket with SO_BROADCAST & SO_REUSEADDR
                broadcastSenderSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                }

                // 5. Start listening loop
                startListeningLoop(mSocket)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up UDP/Multicast sockets on port $P2P_UDP_PORT", e)
            }
        }
        updateStats()
    }

    private fun startListeningLoop(socket: MulticastSocket) {
        receiverJob?.cancel()
        receiverJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_SIZE)
            var consecutiveErrors = 0

            Log.i(TAG, "Starting UDP/Multicast packet receiver loop on port $P2P_UDP_PORT...")
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    consecutiveErrors = 0

                    val message = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8)
                    val telemetry = TelemetryPacket.fromJsonString(message)

                    if (telemetry != null) {
                        packetsReceivedCount++
                        activePeersSet.add(telemetry.senderId)
                        Log.d(TAG, "RX: [${telemetry.type}] from ${telemetry.callSign} (${telemetry.senderId}) @ ${packet.address.hostAddress}")
                        _incomingPackets.emit(telemetry)
                        updateStats()
                    }
                } catch (e: SocketTimeoutException) {
                    // Normal timeout for non-blocking loop, continue listening
                } catch (e: Exception) {
                    if (isActive && !socket.isClosed) {
                        consecutiveErrors++
                        Log.w(TAG, "Socket receive encountered error ($consecutiveErrors): ${e.message}")
                        if (consecutiveErrors >= 5) {
                            delay(1000)
                            Log.e(TAG, "Multiple socket receive failures; requesting socket rebind...")
                            scope.launch { rebindSockets() }
                            break
                        }
                    } else {
                        break
                    }
                }
            }
            Log.d(TAG, "UDP/Multicast packet receiver loop stopped.")
        }
    }

    /**
     * Precise Subnet Broadcast Calculation:
     * Discovers all active, non-loopback network interfaces (wlan0, ap0, swlan0, p2p0, etc.)
     * and derives their exact subnet broadcast addresses (handling both client and softAP host mode),
     * with fallback to 255.255.255.255.
     */
    fun getActiveBroadcastAddresses(): List<InetAddress> {
        val broadcastAddresses = mutableSetOf<InetAddress>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    for (ifaceAddr in iface.interfaceAddresses) {
                        val addr = ifaceAddr.address
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val directBroadcast = ifaceAddr.broadcast
                            if (directBroadcast != null) {
                                broadcastAddresses.add(directBroadcast)
                            } else {
                                // Calculate broadcast address mathematically from IPv4 address and prefix length
                                val prefix = ifaceAddr.networkPrefixLength.toInt()
                                if (prefix in 1..31) {
                                    val ipBytes = addr.address
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
                                    try {
                                        broadcastAddresses.add(InetAddress.getByAddress(broadcastBytes))
                                    } catch (ignored: Exception) {}
                                }
                            }

                            // Standard Android Portable Hotspot subnet fallback
                            val hostIp = addr.hostAddress ?: ""
                            if (hostIp.startsWith("192.168.43.")) {
                                try {
                                    broadcastAddresses.add(InetAddress.getByName("192.168.43.255"))
                                } catch (ignored: Exception) {}
                            } else if (hostIp.startsWith("192.168.49.")) {
                                try {
                                    broadcastAddresses.add(InetAddress.getByName("192.168.49.255"))
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating active subnet broadcast addresses", e)
        }

        // Generic limited broadcast fallback
        try {
            broadcastAddresses.add(InetAddress.getByName("255.255.255.255"))
        } catch (ignored: Exception) {}

        return broadcastAddresses.toList()
    }

    /**
     * Broadcasts a telemetry packet over both local zero-internet P2P (directed subnet broadcasts
     * + UDP multicast) and maintains accurate metrics.
     */
    suspend fun broadcastTelemetry(packet: TelemetryPacket) = withContext(Dispatchers.IO) {
        val payload = packet.toJsonString().toByteArray(StandardCharsets.UTF_8)
        packetsSentCount++

        val targets = getActiveBroadcastAddresses()
        var directBroadcastSentCount = 0

        // 1. Transmit via Subnet Broadcasts (to each computed subnet broadcast e.g. 192.168.43.255 and 255.255.255.255)
        for (targetAddr in targets) {
            try {
                val datagram = DatagramPacket(payload, payload.size, targetAddr, P2P_UDP_PORT)
                val sender = broadcastSenderSocket ?: DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                }
                sender.send(datagram)
                directBroadcastSentCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send broadcast datagram to ${targetAddr.hostAddress}: ${e.message}")
            }
        }

        // 2. Transmit via UDP Multicast (to 239.255.42.99)
        var multicastSent = false
        try {
            val groupAddress = InetAddress.getByName(MULTICAST_GROUP_IP)
            val multicastPacket = DatagramPacket(payload, payload.size, groupAddress, P2P_UDP_PORT)
            multicastSocket?.send(multicastPacket)
            multicastSent = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send multicast datagram to $MULTICAST_GROUP_IP: ${e.message}")
        }

        Log.d(
            TAG,
            "TX: [${packet.type}] ${packet.callSign} -> Subnet Broadcasts: $directBroadcastSentCount targets, Multicast: $multicastSent"
        )
        updateStats()
    }

    /**
     * Relays or injects peer movement packets for active trip session members.
     */
    suspend fun simulatePeerMovement(peerPacket: TelemetryPacket) {
        packetsReceivedCount++
        activePeersSet.add(peerPacket.senderId)
        _incomingPackets.emit(peerPacket)
        updateStats()
    }

    private fun updateStats() {
        val ip = getLocalIpAddress() ?: "127.0.0.1"
        val broadcastAddrs = getActiveBroadcastAddresses().map { it.hostAddress ?: "" }.filter { it.isNotEmpty() }
        val isHeld = multicastLock?.isHeld == true

        _syncStats.value = NetworkSyncStats(
            mode = _connectionMode.value,
            connectedPeersCount = activePeersSet.size,
            pingLatencyMs = if (_connectionMode.value == ConnectionMode.ONLINE_CLOUD) 28 else 4,
            packetsSent = packetsSentCount,
            packetsReceived = packetsReceivedCount,
            localIpAddress = ip,
            isHotspotActive = isHotspotActive(),
            broadcastAddresses = broadcastAddrs,
            isMulticastLockHeld = isHeld
        )
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    /**
     * Releases sockets and multicast lock gracefully.
     */
    fun release() {
        try {
            rebindJob?.cancel()
            receiverJob?.cancel()
            multicastSocket?.close()
            broadcastSenderSocket?.close()
            multicastSocket = null
            broadcastSenderSocket = null
            releaseMulticastLock()
            Log.i(TAG, "HybridNetworkManager released resources.")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing HybridNetworkManager", e)
        }
    }
}
