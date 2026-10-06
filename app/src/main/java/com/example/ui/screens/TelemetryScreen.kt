package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VpnServerEntity
import com.example.data.VpnSettingsEntity
import com.example.model.VpnConnectionState
import com.example.model.VpnTrafficStats
import com.example.ui.theme.*

@Composable
fun TelemetryScreen(
    connectionState: VpnConnectionState,
    trafficStats: VpnTrafficStats,
    server: VpnServerEntity,
    settings: VpnSettingsEntity
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("telemetry_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Column {
            Text(
                text = "WireGuard Telemetry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Real-time cryptometrics & network telemetry",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        // Live Velocity Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp)),
            color = MidnightSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NETWORK THROUGHPUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NeonCyan
                    )

                    Surface(
                        color = if (connectionState.isConnected) ShieldEmerald.copy(alpha = 0.15f) else MidnightSurfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (connectionState.isConnected) "TUNNEL ACTIVE" else "IDLE",
                            color = if (connectionState.isConnected) ShieldEmerald else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Download Speed
                    MetricTile(
                        icon = Icons.Default.ArrowDownward,
                        iconTint = NeonCyan,
                        title = "Download Speed",
                        value = if (connectionState.isConnected) trafficStats.formatDownloadSpeed() else "0.0 KB/s",
                        subtitle = "Total: ${trafficStats.formatBytesIn()}"
                    )

                    // Upload Speed
                    MetricTile(
                        icon = Icons.Default.ArrowUpward,
                        iconTint = ShieldEmerald,
                        title = "Upload Speed",
                        value = if (connectionState.isConnected) trafficStats.formatUploadSpeed() else "0.0 KB/s",
                        subtitle = "Total: ${trafficStats.formatBytesOut()}"
                    )
                }

                Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Session Time", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = trafficStats.formatDuration(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column {
                        Text("Handshake Refresh", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = if (connectionState.isConnected) "${trafficStats.lastHandshakeTimeAgoSeconds}s ago" else "None",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (connectionState.isConnected) ShieldEmerald else TextSecondary
                        )
                    }

                    Column {
                        Text("Active Ping", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "${server.pingMs} ms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // Cryptography Specification Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp)),
            color = MidnightSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WIREGUARD CRYPTO PRIMITIVES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NeonCyan
                )

                SpecRow(label = "Authenticated Encryption", value = "ChaCha20-Poly1305 (RFC 8439)")
                SpecRow(label = "Key Exchange (ECDH)", value = "Curve25519 / X25519")
                SpecRow(label = "Hashing Function", value = "BLAKE2s-256")
                SpecRow(label = "Handshake Protocol", value = "Noise_IK (1-RTT, identity-hiding)")
                SpecRow(label = "Key Rotation Interval", value = "Every 120 seconds or 1GB transfer")
                SpecRow(label = "Perfect Forward Secrecy", value = "Enabled (ephemeral keypairs)")
            }
        }

        // Tunnel Network Routing Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp)),
            color = MidnightSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TUNNEL NETWORK CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NeonCyan
                )

                SpecRow(label = "Assigned Virtual IP", value = trafficStats.virtualIp)
                SpecRow(label = "Remote Peer Endpoint", value = server.endpoint)
                SpecRow(label = "Interface MTU", value = "${settings.mtu} bytes")
                SpecRow(label = "Allowed IPs (Routes)", value = server.allowedIps)
                SpecRow(label = "DNS Resolvers", value = settings.dnsProvider)
                SpecRow(label = "Persistent Keepalive", value = "${settings.persistentKeepalive}s interval")
                SpecRow(label = "Kill Switch Containment", value = if (settings.killSwitchEnabled) "Active & Enforced" else "Disabled")
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    value: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = iconTint)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
