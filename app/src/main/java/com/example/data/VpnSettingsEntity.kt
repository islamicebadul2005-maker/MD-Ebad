package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_settings")
data class VpnSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val killSwitchEnabled: Boolean = true,
    val killSwitchStrictDns: Boolean = true,
    val killSwitchAutoReconnect: Boolean = true,
    val mtu: Int = 1420,
    val dnsProvider: String = "Cloudflare (1.1.1.1)",
    val customDns: String = "1.1.1.1",
    val persistentKeepalive: Int = 25,
    val selectedServerId: String = "us-east-1",
    val splitTunnelingEnabled: Boolean = false,
    val bypassAppPackages: String = "",
    val alwaysOnGuidanceDismissed: Boolean = false
)
