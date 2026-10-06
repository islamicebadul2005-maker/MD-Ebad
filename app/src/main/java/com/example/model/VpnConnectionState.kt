package com.example.model

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    BLOCKED_BY_KILL_SWITCH;

    val isConnected: Boolean
        get() = this == CONNECTED

    val isConnecting: Boolean
        get() = this == CONNECTING

    val isBusy: Boolean
        get() = this == CONNECTING || this == DISCONNECTING

    val label: String
        get() = when (this) {
            DISCONNECTED -> "Disconnected"
            CONNECTING -> "Connecting..."
            CONNECTED -> "Connected"
            DISCONNECTING -> "Disconnecting..."
            BLOCKED_BY_KILL_SWITCH -> "Kill Switch Active"
        }
}
