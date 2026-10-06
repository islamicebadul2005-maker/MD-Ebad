package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.WireGuardConfig

@Entity(tableName = "vpn_servers")
data class VpnServerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val city: String,
    val flagEmoji: String,
    val endpoint: String,
    val publicKey: String,
    val clientAddress: String = "10.14.0.2/32",
    val dnsServers: String = "1.1.1.1, 1.0.0.1",
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val pingMs: Int = 35,
    val serverLoad: Int = 45,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String = "RECOMMENDED", // RECOMMENDED, FAST, STREAMING, P2P, CUSTOM
    val lastUsedTimestamp: Long = 0L
) {
    fun toWireGuardConfig(): WireGuardConfig {
        return WireGuardConfig(
            serverName = name,
            peerEndpoint = endpoint,
            peerPublicKey = publicKey,
            interfaceAddress = clientAddress,
            interfaceDns = dnsServers,
            peerAllowedIps = allowedIps
        )
    }
}
