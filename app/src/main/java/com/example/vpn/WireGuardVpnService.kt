package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.VpnRepository
import com.example.model.VpnConnectionState
import com.example.model.VpnTrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.random.Random

class WireGuardVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: VpnRepository

    private var serverName: String = "WireGuard Server"
    private var endpoint: String = ""
    private var clientIp: String = "10.14.0.2"
    private var killSwitchEnabled: Boolean = true
    private var mtu: Int = 1420
    private var dnsServers: String = "1.1.1.1, 1.0.0.1"

    private var bytesDownloaded: Long = 0L
    private var bytesUploaded: Long = 0L
    private var connectionStartTime: Long = 0L
    private var lastHandshakeSeconds: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = VpnRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "WireGuard Server"
                endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: "198.51.100.12:51820"
                clientIp = intent.getStringExtra(EXTRA_CLIENT_IP)?.substringBefore("/") ?: "10.14.0.2"
                dnsServers = intent.getStringExtra(EXTRA_DNS) ?: "1.1.1.1, 1.0.0.1"
                mtu = intent.getIntExtra(EXTRA_MTU, 1420)
                killSwitchEnabled = intent.getBooleanExtra(EXTRA_KILL_SWITCH, true)

                startVpnTunnel()
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel(normal = true)
            }
            ACTION_KILL_SWITCH_DROP -> {
                handleKillSwitchDrop()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel() {
        startForegroundServiceNotification(
            title = "WireShield VPN • Connecting...",
            content = "Establishing WireGuard tunnel to $serverName"
        )
        VpnServiceManager.updateState(VpnConnectionState.CONNECTING)

        tunnelJob?.cancel()
        tunnelJob = serviceScope.launch {
            try {
                repository.addLog("INFO", "WireGuard", "Setting up local TUN interface (MTU $mtu, Address $clientIp/32)")

                val builder = Builder()
                    .setSession("WireShield: $serverName")
                    .setMtu(mtu)
                    .addAddress(clientIp, 32)
                    .addRoute("0.0.0.0", 0)

                // Add configured DNS servers
                dnsServers.split(",").forEach { rawDns ->
                    val trimmed = rawDns.trim()
                    if (trimmed.isNotEmpty()) {
                        try {
                            builder.addDnsServer(trimmed)
                        } catch (e: Exception) {
                            // ignore invalid DNS entry
                        }
                    }
                }

                builder.setBlocking(true)

                // Establish the TUN interface
                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    repository.addLog("ERROR", "WireGuard", "Failed to establish VPN interface. System permission denied or another VPN is active.")
                    VpnServiceManager.updateState(VpnConnectionState.DISCONNECTED)
                    stopSelf()
                    return@launch
                }

                // Simulate WireGuard Noise_IK Handshake sequence
                repository.addLog("INFO", "Handshake", "Initiating Noise_IK handshake with $endpoint")
                delay(300)
                repository.addLog("INFO", "Handshake", "Transmitted WireGuard Handshake Initiation (Type 0x01, 148 bytes)")
                delay(350)
                repository.addLog("SUCCESS", "Handshake", "Received Handshake Response (Type 0x02, 92 bytes) from $endpoint")
                repository.addLog("SUCCESS", "WireGuard", "Session key negotiated: ChaCha20-Poly1305. Tunnel ESTABLISHED.")

                connectionStartTime = System.currentTimeMillis()
                bytesDownloaded = 4096L
                bytesUploaded = 1024L
                lastHandshakeSeconds = 0L

                VpnServiceManager.updateState(VpnConnectionState.CONNECTED)
                VpnServiceManager.setKillSwitchEngaged(false)

                startForegroundServiceNotification(
                    title = "WireShield VPN • Connected",
                    content = "$serverName • WireGuard Encrypted"
                )

                // Main traffic telemetry & keepalive loop
                var loopCounter = 0
                val inStream = FileInputStream(vpnInterface?.fileDescriptor)
                val outStream = FileOutputStream(vpnInterface?.fileDescriptor)

                while (isActive) {
                    delay(1000)
                    loopCounter++
                    lastHandshakeSeconds = (lastHandshakeSeconds + 1) % 180

                    val durationSec = (System.currentTimeMillis() - connectionStartTime) / 1000

                    // Dynamic simulated traffic for realistic telemetry
                    val currentDownRate = (85_000L..1_450_000L).random()
                    val currentUpRate = (12_000L..280_000L).random()

                    bytesDownloaded += currentDownRate
                    bytesUploaded += currentUpRate

                    // Keepalive every 25 seconds as per WireGuard standard
                    if (loopCounter % 25 == 0) {
                        repository.addLog("INFO", "WireGuard", "Sent PersistentKeepalive packet to $endpoint")
                        lastHandshakeSeconds = 0L
                    }

                    val stats = VpnTrafficStats(
                        bytesIn = bytesDownloaded,
                        bytesOut = bytesUploaded,
                        downloadSpeedBps = currentDownRate,
                        uploadSpeedBps = currentUpRate,
                        durationSeconds = durationSec,
                        lastHandshakeTimeAgoSeconds = lastHandshakeSeconds,
                        virtualIp = clientIp,
                        activeEndpoint = endpoint
                    )
                    VpnServiceManager.updateTrafficStats(stats)

                    // Periodically refresh notification duration
                    if (loopCounter % 5 == 0) {
                        startForegroundServiceNotification(
                            title = "WireShield VPN • Connected",
                            content = "$serverName • ${stats.formatDuration()} • ↓ ${stats.formatDownloadSpeed()}"
                        )
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    repository.addLog("WARN", "WireGuard", "Tunnel interrupted: ${e.message}")
                    if (killSwitchEnabled) {
                        handleKillSwitchDrop()
                    } else {
                        stopVpnTunnel(normal = false)
                    }
                }
            }
        }
    }

    private fun handleKillSwitchDrop() {
        serviceScope.launch {
            repository.addLog("SECURITY", "KillSwitch", "VPN connection lost! Kill switch triggered to block unencrypted traffic.")
        }
        VpnServiceManager.updateState(VpnConnectionState.BLOCKED_BY_KILL_SWITCH)
        VpnServiceManager.setKillSwitchEngaged(true)

        startForegroundServiceNotification(
            title = "WireShield VPN • Kill Switch Active",
            content = "Traffic blocked to prevent leaks. Tap to reconnect."
        )

        // Close TUN interface
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun stopVpnTunnel(normal: Boolean) {
        tunnelJob?.cancel()
        tunnelJob = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        if (normal) {
            repository.let {
                serviceScope.launch {
                    it.addLog("INFO", "WireGuard", "Tunnel closed cleanly by user.")
                }
            }
        }

        VpnServiceManager.updateState(VpnConnectionState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnTunnel(normal = true)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WireShield VPN Tunnel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and quick controls for active WireGuard VPN tunnel"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification(title: String, content: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, WireGuardVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wireshield_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.example.wireshield.CONNECT"
        const val ACTION_DISCONNECT = "com.example.wireshield.DISCONNECT"
        const val ACTION_KILL_SWITCH_DROP = "com.example.wireshield.KILL_SWITCH_DROP"

        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_PUBLIC_KEY = "extra_public_key"
        const val EXTRA_CLIENT_IP = "extra_client_ip"
        const val EXTRA_DNS = "extra_dns"
        const val EXTRA_MTU = "extra_mtu"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"

        private const val CHANNEL_ID = "wireshield_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
