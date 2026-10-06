package com.example.model

import java.security.SecureRandom
import android.util.Base64

data class WireGuardConfig(
    val serverName: String = "WireGuard Server",
    val interfacePrivateKey: String = generateRandomKey(),
    val interfaceAddress: String = "10.14.0.2/32",
    val interfaceDns: String = "1.1.1.1, 1.0.0.1",
    val interfaceMtu: Int = 1420,
    val peerPublicKey: String = generateRandomKey(),
    val peerPresharedKey: String? = null,
    val peerEndpoint: String = "198.51.100.12:51820",
    val peerAllowedIps: String = "0.0.0.0/0, ::/0",
    val peerPersistentKeepalive: Int = 25
) {
    fun toConfString(): String {
        val sb = StringBuilder()
        sb.append("[Interface]\n")
        sb.append("PrivateKey = ").append(interfacePrivateKey).append("\n")
        sb.append("Address = ").append(interfaceAddress).append("\n")
        if (interfaceDns.isNotBlank()) {
            sb.append("DNS = ").append(interfaceDns).append("\n")
        }
        sb.append("MTU = ").append(interfaceMtu).append("\n\n")

        sb.append("[Peer]\n")
        sb.append("PublicKey = ").append(peerPublicKey).append("\n")
        if (!peerPresharedKey.isNullOrBlank()) {
            sb.append("PresharedKey = ").append(peerPresharedKey).append("\n")
        }
        sb.append("Endpoint = ").append(peerEndpoint).append("\n")
        sb.append("AllowedIPs = ").append(peerAllowedIps).append("\n")
        if (peerPersistentKeepalive > 0) {
            sb.append("PersistentKeepalive = ").append(peerPersistentKeepalive).append("\n")
        }
        return sb.toString()
    }

    companion object {
        fun generateRandomKey(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        fun parseConf(confText: String, defaultName: String = "Imported WireGuard"): WireGuardConfig {
            var privateKey = ""
            var address = "10.14.0.2/32"
            var dns = "1.1.1.1, 1.0.0.1"
            var mtu = 1420
            var publicKey = ""
            var presharedKey: String? = null
            var endpoint = "198.51.100.12:51820"
            var allowedIps = "0.0.0.0/0, ::/0"
            var keepalive = 25

            var currentSection = ""

            confText.lines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("#") || line.isBlank()) return@forEach

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length - 1).trim().lowercase()
                    return@forEach
                }

                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()

                    when (currentSection) {
                        "interface" -> {
                            when (key) {
                                "privatekey" -> privateKey = value
                                "address" -> address = value
                                "dns" -> dns = value
                                "mtu" -> mtu = value.toIntOrNull() ?: 1420
                            }
                        }
                        "peer" -> {
                            when (key) {
                                "publickey" -> publicKey = value
                                "presharedkey" -> presharedKey = value
                                "endpoint" -> endpoint = value
                                "allowedips" -> allowedIps = value
                                "persistentkeepalive" -> keepalive = value.toIntOrNull() ?: 25
                            }
                        }
                    }
                }
            }

            return WireGuardConfig(
                serverName = defaultName,
                interfacePrivateKey = privateKey.ifBlank { generateRandomKey() },
                interfaceAddress = address,
                interfaceDns = dns,
                interfaceMtu = mtu,
                peerPublicKey = publicKey.ifBlank { generateRandomKey() },
                peerPresharedKey = presharedKey,
                peerEndpoint = endpoint,
                peerAllowedIps = allowedIps,
                peerPersistentKeepalive = keepalive
            )
        }
    }
}
