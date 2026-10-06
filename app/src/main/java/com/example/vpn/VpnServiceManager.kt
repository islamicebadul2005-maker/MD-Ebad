package com.example.vpn

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.VpnServerEntity
import com.example.model.VpnConnectionState
import com.example.model.VpnTrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnServiceManager {
    private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _trafficStats = MutableStateFlow(VpnTrafficStats())
    val trafficStats: StateFlow<VpnTrafficStats> = _trafficStats.asStateFlow()

    private val _activeServer = MutableStateFlow<VpnServerEntity?>(null)
    val activeServer: StateFlow<VpnServerEntity?> = _activeServer.asStateFlow()

    private val _isKillSwitchActive = MutableStateFlow(false)
    val isKillSwitchActive: StateFlow<Boolean> = _isKillSwitchActive.asStateFlow()

    fun updateState(newState: VpnConnectionState) {
        _connectionState.value = newState
        if (newState == VpnConnectionState.DISCONNECTED) {
            _trafficStats.value = VpnTrafficStats()
            _isKillSwitchActive.value = false
        }
    }

    fun updateTrafficStats(stats: VpnTrafficStats) {
        _trafficStats.value = stats
    }

    fun setActiveServer(server: VpnServerEntity?) {
        _activeServer.value = server
    }

    fun setKillSwitchEngaged(engaged: Boolean) {
        _isKillSwitchActive.value = engaged
    }

    fun startVpn(
        context: Context,
        server: VpnServerEntity,
        killSwitchEnabled: Boolean,
        mtu: Int = 1420,
        dns: String = "1.1.1.1, 1.0.0.1"
    ) {
        setActiveServer(server)
        _connectionState.value = VpnConnectionState.CONNECTING

        val intent = Intent(context, WireGuardVpnService::class.java).apply {
            action = WireGuardVpnService.ACTION_CONNECT
            putExtra(WireGuardVpnService.EXTRA_SERVER_ID, server.id)
            putExtra(WireGuardVpnService.EXTRA_SERVER_NAME, server.name)
            putExtra(WireGuardVpnService.EXTRA_ENDPOINT, server.endpoint)
            putExtra(WireGuardVpnService.EXTRA_PUBLIC_KEY, server.publicKey)
            putExtra(WireGuardVpnService.EXTRA_CLIENT_IP, server.clientAddress)
            putExtra(WireGuardVpnService.EXTRA_DNS, dns)
            putExtra(WireGuardVpnService.EXTRA_MTU, mtu)
            putExtra(WireGuardVpnService.EXTRA_KILL_SWITCH, killSwitchEnabled)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpn(context: Context) {
        _connectionState.value = VpnConnectionState.DISCONNECTING
        val intent = Intent(context, WireGuardVpnService::class.java).apply {
            action = WireGuardVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun triggerKillSwitchContainment(context: Context) {
        _connectionState.value = VpnConnectionState.BLOCKED_BY_KILL_SWITCH
        _isKillSwitchActive.value = true
        val intent = Intent(context, WireGuardVpnService::class.java).apply {
            action = WireGuardVpnService.ACTION_KILL_SWITCH_DROP
        }
        context.startService(intent)
    }
}
