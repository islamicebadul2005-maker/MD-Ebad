package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VpnServerEntity::class,
        VpnLogEntity::class,
        VpnSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vpnDao(): VpnDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wireshield_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial servers & default settings
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).vpnDao()
                                dao.insertDefaultServers(DefaultServers.list)
                                dao.insertSettings(VpnSettingsEntity())
                                dao.insertLog(
                                    VpnLogEntity(
                                        level = "SUCCESS",
                                        tag = "System",
                                        message = "WireShield database initialized. Ready for WireGuard tunnels."
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
