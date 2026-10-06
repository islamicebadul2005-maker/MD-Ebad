package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VpnSettingsEntity
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: VpnSettingsEntity,
    onToggleKillSwitch: (Boolean) -> Unit,
    onToggleStrictDns: (Boolean) -> Unit,
    onUpdateMtu: (Int) -> Unit,
    onUpdateDns: (provider: String, custom: String) -> Unit,
    onOpenKillSwitchGuide: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showMtuDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Column {
            Text(
                text = "Security & Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Manage WireGuard tunnel and leak protection parameters",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        // KILL SWITCH & SECURITY SECTION
        SettingsSection(title = "KILL SWITCH & LEAK PROTECTION") {
            SettingsSwitchRow(
                icon = Icons.Default.Shield,
                iconTint = DangerRed,
                title = "WireGuard Kill Switch",
                subtitle = "Immediately cut off internet if VPN drops unexpectedly",
                checked = settings.killSwitchEnabled,
                onCheckedChange = onToggleKillSwitch,
                tag = "settings_kill_switch_toggle"
            )

            Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            SettingsSwitchRow(
                icon = Icons.Default.Dns,
                iconTint = NeonCyan,
                title = "Strict DNS Leak Shield",
                subtitle = "Force all domain name resolution inside encrypted tunnel",
                checked = settings.killSwitchStrictDns,
                onCheckedChange = onToggleStrictDns,
                tag = "settings_strict_dns_toggle"
            )

            Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            SettingsClickableRow(
                icon = Icons.Default.Security,
                iconTint = WarningAmber,
                title = "System Always-on VPN Guide",
                subtitle = "Lock down device at OS level via Android settings",
                value = "View Guide",
                onClick = onOpenKillSwitchGuide,
                tag = "settings_always_on_guide"
            )
        }

        // WIREGUARD PROTOCOL PARAMETERS
        SettingsSection(title = "WIREGUARD PROTOCOL TUNING") {
            SettingsClickableRow(
                icon = Icons.Default.SettingsEthernet,
                iconTint = NeonCyan,
                title = "Interface MTU Size",
                subtitle = "Maximum Transmission Unit for packet encapsulation",
                value = "${settings.mtu} bytes",
                onClick = { showMtuDialog = true },
                tag = "settings_mtu_row"
            )

            Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            SettingsClickableRow(
                icon = Icons.Default.Language,
                iconTint = ShieldEmerald,
                title = "DNS Resolvers",
                subtitle = "Encrypted upstream nameservers for DNS queries",
                value = settings.dnsProvider.substringBefore(" ("),
                onClick = { showDnsDialog = true },
                tag = "settings_dns_row"
            )

            Divider(color = MidnightCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Persistent Keepalive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Keeps NAT state open every 25 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = "${settings.persistentKeepalive}s",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        // ABOUT & PRIVACY
        SettingsSection(title = "PROTOCOL & PRIVACY") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "WireGuard® is an extremely simple yet fast and modern VPN that utilizes state-of-the-art cryptography. It aims to be faster, simpler, leaner, and more useful than IPsec and OpenVPN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Zero-Logs Guarantee: WireShield runs no telemetry tracking or connection logging. WireGuard state exists purely in volatile memory for active sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShieldEmerald,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // MTU Selection Dialog
    if (showMtuDialog) {
        val mtuOptions = listOf(1280, 1360, 1420, 1500)
        AlertDialog(
            onDismissRequest = { showMtuDialog = false },
            containerColor = MidnightSurface,
            title = {
                Text("Select Interface MTU", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "1420 is the recommended standard for WireGuard to prevent IPv4/IPv6 packet fragmentation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    mtuOptions.forEach { mtu ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onUpdateMtu(mtu)
                                    showMtuDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$mtu bytes ${if (mtu == 1420) "(Default)" else ""}",
                                color = if (mtu == settings.mtu) NeonCyan else TextPrimary,
                                fontWeight = if (mtu == settings.mtu) FontWeight.Bold else FontWeight.Normal
                            )
                            if (mtu == settings.mtu) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMtuDialog = false }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }

    // DNS Selection Dialog
    if (showDnsDialog) {
        val dnsOptions = listOf(
            "Cloudflare (1.1.1.1)" to "1.1.1.1, 1.0.0.1",
            "Quad9 (9.9.9.9)" to "9.9.9.9, 149.112.112.112",
            "AdGuard (94.140.14.14)" to "94.140.14.14, 94.140.15.15",
            "Custom" to settings.customDns
        )
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            containerColor = MidnightSurface,
            title = {
                Text("Select DNS Resolvers", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dnsOptions.forEach { (name, ips) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onUpdateDns(name, ips)
                                    showDnsDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    color = if (settings.dnsProvider == name) NeonCyan else TextPrimary,
                                    fontWeight = if (settings.dnsProvider == name) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = ips,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            if (settings.dnsProvider == name) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = NeonCyan,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MidnightCardBorder, RoundedCornerShape(18.dp)),
            color = MidnightSurface
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = iconTint,
                checkedTrackColor = iconTint.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = MidnightSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
