package com.example.model

data class VpnTrafficStats(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val durationSeconds: Long = 0L,
    val lastHandshakeTimeAgoSeconds: Long = 0L,
    val virtualIp: String = "10.14.0.2",
    val cipher: String = "ChaCha20-Poly1305",
    val keyExchange: String = "Curve25519 (ECDH)",
    val hashing: String = "BLAKE2s",
    val activeEndpoint: String = ""
) {
    fun formatDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatDownloadSpeed(): String = formatSpeed(downloadSpeedBps)
    fun formatUploadSpeed(): String = formatSpeed(uploadSpeedBps)

    fun formatBytesIn(): String = formatBytes(bytesIn)
    fun formatBytesOut(): String = formatBytes(bytesOut)

    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
                bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
                else -> "$bytesPerSec B/s"
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}
