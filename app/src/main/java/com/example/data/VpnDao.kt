package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {
    // Servers
    @Query("SELECT * FROM vpn_servers ORDER BY isFavorite DESC, pingMs ASC")
    fun getAllServers(): Flow<List<VpnServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: String): VpnServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VpnServerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultServers(servers: List<VpnServerEntity>)

    @Update
    suspend fun updateServer(server: VpnServerEntity)

    @Query("DELETE FROM vpn_servers WHERE id = :id")
    suspend fun deleteServer(id: String)

    @Query("UPDATE vpn_servers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setServerFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE vpn_servers SET pingMs = :pingMs WHERE id = :id")
    suspend fun updateServerPing(id: String, pingMs: Int)

    // Logs
    @Query("SELECT * FROM vpn_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<VpnLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VpnLogEntity)

    @Query("DELETE FROM vpn_logs")
    suspend fun clearLogs()

    // Settings
    @Query("SELECT * FROM vpn_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<VpnSettingsEntity?>

    @Query("SELECT * FROM vpn_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): VpnSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: VpnSettingsEntity)
}
