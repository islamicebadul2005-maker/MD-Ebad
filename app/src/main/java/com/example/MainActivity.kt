package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.VpnConnectionState
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.VpnViewModel

enum class MainTab(val title: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Default.Shield, "tab_home"),
    SERVERS("Servers", Icons.Default.Dns, "tab_servers"),
    TELEMETRY("Telemetry", Icons.Default.Speed, "tab_telemetry"),
    LOGS("Logs", Icons.Default.Terminal, "tab_logs"),
    SETTINGS("Settings", Icons.Default.Tune, "tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: VpnViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var showKillSwitchDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Collect reactive state flows
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    // Activity result launcher for VpnService.prepare()
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted(context)
        }
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* granted / denied handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle back button on sub-screens
    if (currentTab != MainTab.HOME) {
        BackHandler {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = MidnightBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MidnightSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(0.8.dp, MidnightCardBorder)
                    .testTag("main_navigation_bar")
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) MidnightBg else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan,
                            selectedIconColor = MidnightBg,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = NeonCyan,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        connectionState = connectionState,
                        selectedServer = selectedServer,
                        trafficStats = trafficStats,
                        settings = settings,
                        onToggleConnect = {
                            viewModel.toggleConnect(context) { prepareIntent ->
                                vpnPrepareLauncher.launch(prepareIntent)
                            }
                        },
                        onToggleKillSwitch = { viewModel.toggleKillSwitch(it) },
                        onNavigateToServers = { currentTab = MainTab.SERVERS },
                        onOpenKillSwitchDialog = { showKillSwitchDialog = true }
                    )
                }
                MainTab.SERVERS -> {
                    ServerListScreen(
                        servers = allServers,
                        selectedServer = selectedServer,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        isPinging = isPinging,
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onCategoryChanged = { viewModel.setSelectedCategory(it) },
                        onSelectServer = { server ->
                            viewModel.selectServer(server)
                            currentTab = MainTab.HOME
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRefreshPings = { viewModel.refreshAllPings() },
                        onOpenImportDialog = { showImportDialog = true },
                        onDeleteCustomServer = { viewModel.deleteCustomServer(it) }
                    )
                }
                MainTab.TELEMETRY -> {
                    TelemetryScreen(
                        connectionState = connectionState,
                        trafficStats = trafficStats,
                        server = selectedServer,
                        settings = settings
                    )
                }
                MainTab.LOGS -> {
                    LogsScreen(
                        logs = recentLogs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onToggleKillSwitch = { viewModel.toggleKillSwitch(it) },
                        onToggleStrictDns = { viewModel.toggleStrictDns(it) },
                        onUpdateMtu = { viewModel.updateMtu(it) },
                        onUpdateDns = { provider, custom -> viewModel.updateDns(provider, custom) },
                        onOpenKillSwitchGuide = { showKillSwitchDialog = true }
                    )
                }
            }
        }
    }

    // Kill switch modal dialog
    if (showKillSwitchDialog) {
        KillSwitchDialog(
            onDismiss = { showKillSwitchDialog = false },
            onSimulateDrop = {
                viewModel.simulateKillSwitchDrop(context)
            }
        )
    }

    // Import WireGuard .conf dialog
    if (showImportDialog) {
        ImportConfigDialog(
            onDismiss = { showImportDialog = false },
            onImport = { confText, name ->
                viewModel.importConf(confText, name)
            }
        )
    }
}
