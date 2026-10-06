package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VpnServerEntity
import com.example.data.VpnSettingsEntity
import com.example.model.VpnConnectionState
import com.example.model.VpnTrafficStats
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    connectionState: VpnConnectionState,
    selectedServer: VpnServerEntity,
    trafficStats: VpnTrafficStats,
    settings: VpnSettingsEntity,
    onToggleConnect: () -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onNavigateToServers: () -> Unit,
    onOpenKillSwitchDialog: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, NeonCyanDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "WireShield Logo",
                        tint = MidnightBg,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "WireShield",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "WireGuard® Core",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }

            // Kill Switch Status Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenKillSwitchDialog() }
                    .border(
                        1.dp,
                        if (settings.killSwitchEnabled) DangerRed.copy(alpha = 0.5f) else MidnightCardBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("home_kill_switch_pill"),
                color = if (settings.killSwitchEnabled) DangerRed.copy(alpha = 0.12f) else MidnightSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (settings.killSwitchEnabled) DangerRed else TextMuted)
                    )
                    Text(
                        text = if (settings.killSwitchEnabled) "Kill Switch ON" else "Kill Switch OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (settings.killSwitchEnabled) DangerRed else TextSecondary
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Kill switch details",
                        tint = if (settings.killSwitchEnabled) DangerRed else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big Connect Button with radar pulse animation
        ConnectButton(
            connectionState = connectionState,
            onClick = onToggleConnect
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status description text
        Text(
            text = when (connectionState) {
                VpnConnectionState.CONNECTED -> "SECURE & ENCRYPTED"
                VpnConnectionState.CONNECTING -> "ESTABLISHING WIREGUARD TUNNEL..."
                VpnConnectionState.DISCONNECTING -> "TEARING DOWN TUNNEL..."
                VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> "KILL SWITCH BLOCKED TRAFFIC"
                VpnConnectionState.DISCONNECTED -> "NOT PROTECTED"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = when (connectionState) {
                VpnConnectionState.CONNECTED -> ShieldEmerald
                VpnConnectionState.CONNECTING -> NeonCyan
                VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> DangerRed
                else -> TextSecondary
            }
        )

        Text(
            text = when (connectionState) {
                VpnConnectionState.CONNECTED -> "Noise_IK Handshake • ChaCha20-Poly1305"
                VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> "Internet cut off to prevent unencrypted IP leak"
                else -> "Curve25519 • Zero-Log Architecture"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Active Server Card
        ServerCard(
            server = selectedServer,
            isConnected = connectionState == VpnConnectionState.CONNECTED,
            onChangeClicked = onNavigateToServers
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Kill Switch Control Card
        KillSwitchCard(
            enabled = settings.killSwitchEnabled,
            onToggle = onToggleKillSwitch,
            onOpenInfo = onOpenKillSwitchDialog
        )

        // Live Telemetry section (Visible when Connected)
        AnimatedVisibility(
            visible = connectionState == VpnConnectionState.CONNECTED
        ) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                LiveStatsCard(trafficStats = trafficStats, server = selectedServer)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ConnectButton(
    connectionState: VpnConnectionState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnConnectionState.CONNECTED -> ShieldEmerald
            VpnConnectionState.CONNECTING -> NeonCyan
            VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> DangerRed
            VpnConnectionState.DISCONNECTING -> WarningAmber
            VpnConnectionState.DISCONNECTED -> MidnightSurfaceVariant
        },
        label = "btnColor"
    )

    val glowColor = when (connectionState) {
        VpnConnectionState.CONNECTED -> ShieldEmerald
        VpnConnectionState.CONNECTING -> NeonCyan
        VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> DangerRed
        else -> NeonCyan
    }

    Box(
        modifier = Modifier
            .size(210.dp)
            .testTag("vpn_connect_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing radar ring if active/busy
        if (connectionState.isConnected || connectionState.isBusy || connectionState == VpnConnectionState.BLOCKED_BY_KILL_SWITCH) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = pulseAlpha))
            )
        }

        // Secondary inner border ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = buttonColor.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Main tactile button
        Surface(
            modifier = Modifier
                .size(154.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .border(2.dp, buttonColor, CircleShape)
                .testTag("vpn_connect_button"),
            color = MidnightSurface,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (connectionState) {
                        VpnConnectionState.CONNECTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(38.dp),
                                color = NeonCyan,
                                strokeWidth = 3.dp
                            )
                        }
                        VpnConnectionState.CONNECTED -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Connected Lock",
                                tint = ShieldEmerald,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Kill switch active",
                                tint = DangerRed,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Connect VPN",
                                tint = NeonCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (connectionState) {
                            VpnConnectionState.CONNECTED -> "DISCONNECT"
                            VpnConnectionState.CONNECTING -> "CONNECTING"
                            VpnConnectionState.DISCONNECTING -> "STOPPING"
                            VpnConnectionState.BLOCKED_BY_KILL_SWITCH -> "RECONNECT"
                            VpnConnectionState.DISCONNECTED -> "CONNECT"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = buttonColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: VpnServerEntity,
    isConnected: Boolean,
    onChangeClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onChangeClicked() }
            .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp))
            .testTag("active_server_card"),
        color = MidnightSurface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = server.flagEmoji,
                    fontSize = 32.sp
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ping indicator
                        val pingColor = when {
                            server.pingMs < 40 -> ShieldEmerald
                            server.pingMs < 80 -> WarningAmber
                            else -> DangerRed
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Text(
                            text = "${server.pingMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = pingColor
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            text = "Load ${server.serverLoad}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onChangeClicked,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MidnightCardBorder)
                ),
                modifier = Modifier.testTag("change_server_button")
            ) {
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun KillSwitchCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenInfo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (enabled) DangerRed.copy(alpha = 0.35f) else MidnightCardBorder,
                RoundedCornerShape(18.dp)
            )
            .testTag("kill_switch_control_card"),
        color = MidnightSurface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (enabled) DangerRed.copy(alpha = 0.15f) else MidnightSurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Kill switch",
                        tint = if (enabled) DangerRed else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Kill Switch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = onOpenInfo,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Kill switch guide",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (enabled) "Blocks leaks if tunnel drops" else "Disabled — traffic can leak",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) DangerRed else TextMuted
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("kill_switch_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DangerRed,
                    checkedTrackColor = DangerRed.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = MidnightSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun LiveStatsCard(
    trafficStats: VpnTrafficStats,
    server: VpnServerEntity
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp))
            .testTag("live_stats_card"),
        color = MidnightSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ShieldEmerald)
                    )
                    Text(
                        text = "LIVE WIRESHIELD TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ShieldEmerald
                    )
                }

                Text(
                    text = trafficStats.formatDuration(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download speed",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = trafficStats.formatDownloadSpeed(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Total: ${trafficStats.formatBytesIn()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // Upload
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload speed",
                            tint = ShieldEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Upload",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = trafficStats.formatUploadSpeed(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ShieldEmerald
                    )
                    Text(
                        text = "Total: ${trafficStats.formatBytesOut()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // Virtual IP
                Column {
                    Text(
                        text = "Tunnel IP",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = trafficStats.virtualIp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Handshake: ${trafficStats.lastHandshakeTimeAgoSeconds}s ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
