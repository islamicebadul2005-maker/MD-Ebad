package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultServers
import com.example.data.VpnLogEntity
import com.example.data.VpnRepository
import com.example.data.VpnServerEntity
import com.example.data.VpnSettingsEntity
import com.example.model.VpnConnectionState
import com.example.model.VpnTrafficStats
import com.example.model.WireGuardConfig
import com.example.vpn.VpnServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VpnRepository(application)

    val connectionState: StateFlow<VpnConnectionState> = VpnServiceManager.connectionState
    val trafficStats: StateFlow<VpnTrafficStats> = VpnServiceManager.trafficStats
    val activeServer: StateFlow<VpnServerEntity?> = VpnServiceManager.activeServer

    val allServers: StateFlow<List<VpnServerEntity>> = repository.allServers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultServers.list
        )

    val recentLogs: StateFlow<List<VpnLogEntity>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _settings = MutableStateFlow(VpnSettingsEntity())
    val settings: StateFlow<VpnSettingsEntity> = _settings.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Currently selected server (defaults to us-east-1 or saved in settings)
    val selectedServer: StateFlow<VpnServerEntity> = combine(
        allServers,
        settings,
        activeServer
    ) { servers, currentSettings, active ->
        if (active != null) {
            active
        } else {
            servers.find { it.id == currentSettings.selectedServerId }
                ?: servers.firstOrNull()
                ?: DefaultServers.list.first()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DefaultServers.list.first()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
            repository.settingsFlow.collect { savedSettings ->
                if (savedSettings != null) {
                    _settings.value = savedSettings
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectServer(server: VpnServerEntity) {
        viewModelScope.launch {
            repository.setSelectedServerId(server.id)
            _settings.value = _settings.value.copy(selectedServerId = server.id)
            if (connectionState.value == VpnConnectionState.CONNECTED) {
                // If already connected, switch to new server
                repository.addLog("INFO", "WireGuard", "Switching tunnel to ${server.name}...")
            }
        }
    }

    fun toggleFavorite(server: VpnServerEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(server.id, server.isFavorite)
        }
    }

    fun toggleKillSwitch(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(killSwitchEnabled = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
            repository.addLog(
                "SECURITY",
                "KillSwitch",
                if (enabled) "Kill switch activated. Unencrypted leaks will be blocked."
                else "Kill switch deactivated."
            )
        }
    }

    fun toggleStrictDns(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(killSwitchStrictDns = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun updateMtu(newMtu: Int) {
        viewModelScope.launch {
            val updated = _settings.value.copy(mtu = newMtu)
            _settings.value = updated
            repository.updateSettings(updated)
            repository.addLog("INFO", "Config", "WireGuard MTU set to $newMtu")
        }
    }

    fun updateDns(provider: String, customDns: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(dnsProvider = provider, customDns = customDns)
            _settings.value = updated
            repository.updateSettings(updated)
            repository.addLog("INFO", "Config", "DNS provider set to $provider ($customDns)")
        }
    }

    fun toggleConnect(context: Context, onPrepareNeeded: (Intent) -> Unit) {
        when (connectionState.value) {
            VpnConnectionState.CONNECTED -> {
                disconnect(context)
            }
            VpnConnectionState.DISCONNECTED,
            VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> {
                connect(context, onPrepareNeeded)
            }
            VpnConnectionState.CONNECTING,
            VpnConnectionState.DISCONNECTING -> {
                // Ignore while in transition
            }
        }
    }

    fun connect(context: Context, onPrepareNeeded: (Intent) -> Unit) {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            onPrepareNeeded(prepareIntent)
            return
        }
        executeStartVpn(context)
    }

    fun onVpnPermissionGranted(context: Context) {
        executeStartVpn(context)
    }

    private fun executeStartVpn(context: Context) {
        val server = selectedServer.value
        val currentSettings = _settings.value

        val effectiveDns = when (currentSettings.dnsProvider) {
            "Cloudflare (1.1.1.1)" -> "1.1.1.1, 1.0.0.1"
            "Quad9 (9.9.9.9)" -> "9.9.9.9, 149.112.112.112"
            "AdGuard (94.140.14.14)" -> "94.140.14.14, 94.140.15.15"
            else -> currentSettings.customDns.ifBlank { "1.1.1.1" }
        }

        VpnServiceManager.startVpn(
            context = context,
            server = server,
            killSwitchEnabled = currentSettings.killSwitchEnabled,
            mtu = currentSettings.mtu,
            dns = effectiveDns
        )
    }

    fun disconnect(context: Context) {
        VpnServiceManager.stopVpn(context)
    }

    fun simulateKillSwitchDrop(context: Context) {
        VpnServiceManager.triggerKillSwitchContainment(context)
    }

    fun refreshAllPings() {
        if (_isPinging.value) return
        viewModelScope.launch {
            _isPinging.value = true
            allServers.value.forEach { server ->
                repository.pingServer(server)
            }
            _isPinging.value = false
        }
    }

    fun importConf(confText: String, name: String) {
        viewModelScope.launch {
            try {
                val parsed = WireGuardConfig.parseConf(confText, name.ifBlank { "Imported Server" })
                val entity = VpnServerEntity(
                    id = "custom-${System.currentTimeMillis()}",
                    name = parsed.serverName,
                    countryCode = "XX",
                    countryName = "Custom Tunnel",
                    city = "Custom Node",
                    flagEmoji = "⚡",
                    endpoint = parsed.peerEndpoint,
                    publicKey = parsed.peerPublicKey,
                    clientAddress = parsed.interfaceAddress,
                    dnsServers = parsed.interfaceDns,
                    allowedIps = parsed.peerAllowedIps,
                    pingMs = 38,
                    serverLoad = 15,
                    isCustom = true,
                    isFavorite = true,
                    category = "CUSTOM"
                )
                repository.addServer(entity)
                selectServer(entity)
            } catch (e: Exception) {
                repository.addLog("ERROR", "Import", "Failed to parse WireGuard config: ${e.message}")
            }
        }
    }

    fun deleteCustomServer(serverId: String) {
        viewModelScope.launch {
            repository.deleteServer(serverId)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
