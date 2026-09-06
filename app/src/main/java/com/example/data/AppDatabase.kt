package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        OrchardEntity::class,
        WorkPartEntity::class,
        SocialPostEntity::class,
        PostCommentEntity::class,
        CalendarTaskEntity::class,
        CustomReminderEntity::class,
        NotificationSettingsEntity::class,
        UserProfileEntity::class,
        IrrigationScheduleEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orchardDao(): OrchardDao
    abstract fun workPartDao(): WorkPartDao
    abstract fun socialPostDao(): SocialPostDao
    abstract fun postCommentDao(): PostCommentDao
    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun customReminderDao(): CustomReminderDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun irrigationScheduleDao(): IrrigationScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getExistingColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
            val columns = mutableSetOf<String>()
            try {
                db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        while (cursor.moveToNext()) {
                            columns.add(cursor.getString(nameIndex))
                        }
                    }
                }
            } catch (_: Exception) {}
            return columns
        }

        private fun ensureAllTablesAndColumns(db: SupportSQLiteDatabase) {
            try {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `irrigation_schedules` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`orchardId` INTEGER NOT NULL, " +
                    "`dayOfWeek` TEXT NOT NULL, " +
                    "`startTime` TEXT NOT NULL, " +
                    "`endTime` TEXT NOT NULL, " +
                    "`isEnabled` INTEGER NOT NULL)"
                )
            } catch (_: Exception) {}

            val notifCols = getExistingColumns(db, "notification_settings")
            if (notifCols.isNotEmpty()) {
                val notificationColumns = listOf(
                    "rainAlert" to "INTEGER NOT NULL DEFAULT 1",
                    "rainProbabilityThreshold" to "TEXT NOT NULL DEFAULT '60'",
                    "hailAlert" to "INTEGER NOT NULL DEFAULT 1",
                    "hailProbabilityThreshold" to "TEXT NOT NULL DEFAULT '40'",
                    "weeklyCostSummary" to "INTEGER NOT NULL DEFAULT 1",
                    "monthlyCostSummary" to "INTEGER NOT NULL DEFAULT 1",
                    "weeklyCostSummaryVyr" to "INTEGER NOT NULL DEFAULT 1",
                    "monthlyCostSummaryVyr" to "INTEGER NOT NULL DEFAULT 1",
                    "weeklyCostSummaryOtros" to "INTEGER NOT NULL DEFAULT 1",
                    "monthlyCostSummaryOtros" to "INTEGER NOT NULL DEFAULT 1",
                    "newSocialPosts" to "INTEGER NOT NULL DEFAULT 1",
                    "frostNotify7Days" to "INTEGER NOT NULL DEFAULT 1",
                    "frostNotify2Days" to "INTEGER NOT NULL DEFAULT 1",
                    "frostNotifyRealtime" to "INTEGER NOT NULL DEFAULT 1",
                    "windNotify7Days" to "INTEGER NOT NULL DEFAULT 1",
                    "windNotify2Days" to "INTEGER NOT NULL DEFAULT 1",
                    "windNotifyRealtime" to "INTEGER NOT NULL DEFAULT 1",
                    "heatNotify7Days" to "INTEGER NOT NULL DEFAULT 1",
                    "heatNotify2Days" to "INTEGER NOT NULL DEFAULT 1",
                    "heatNotifyRealtime" to "INTEGER NOT NULL DEFAULT 1",
                    "rainNotify7Days" to "INTEGER NOT NULL DEFAULT 1",
                    "rainNotify2Days" to "INTEGER NOT NULL DEFAULT 1",
                    "rainNotifyRealtime" to "INTEGER NOT NULL DEFAULT 1",
                    "hailNotify7Days" to "INTEGER NOT NULL DEFAULT 1",
                    "hailNotify2Days" to "INTEGER NOT NULL DEFAULT 1",
                    "hailNotifyRealtime" to "INTEGER NOT NULL DEFAULT 1"
                )
                for ((colName, colDef) in notificationColumns) {
                    if (!notifCols.contains(colName)) {
                        try {
                            db.execSQL("ALTER TABLE `notification_settings` ADD COLUMN `$colName` $colDef")
                        } catch (_: Exception) {}
                    }
                }
            }

            val workPartCols = getExistingColumns(db, "work_parts")
            if (workPartCols.isNotEmpty()) {
                if (!workPartCols.contains("ownerCategory")) {
                    try {
                        db.execSQL("ALTER TABLE `work_parts` ADD COLUMN `ownerCategory` TEXT NOT NULL DEFAULT 'Mío'")
                    } catch (_: Exception) {}
                }
                if (!workPartCols.contains("kilosDestrio")) {
                    try {
                        db.execSQL("ALTER TABLE `work_parts` ADD COLUMN `kilosDestrio` REAL NOT NULL DEFAULT 0.0")
                    } catch (_: Exception) {}
                }
                if (!workPartCols.contains("precioDestrio")) {
                    try {
                        db.execSQL("ALTER TABLE `work_parts` ADD COLUMN `precioDestrio` REAL NOT NULL DEFAULT 0.0")
                    } catch (_: Exception) {}
                }
                if (!workPartCols.contains("indemnizacionSeguro")) {
                    try {
                        db.execSQL("ALTER TABLE `work_parts` ADD COLUMN `indemnizacionSeguro` REAL NOT NULL DEFAULT 0.0")
                    } catch (_: Exception) {}
                }
            }

            val orchardCols = getExistingColumns(db, "orchards")
            if (orchardCols.isNotEmpty()) {
                if (!orchardCols.contains("sortOrder")) {
                    try {
                        db.execSQL("ALTER TABLE `orchards` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {}
                }
                if (!orchardCols.contains("rootstock")) {
                    try {
                        db.execSQL("ALTER TABLE `orchards` ADD COLUMN `rootstock` TEXT NOT NULL DEFAULT ''")
                    } catch (_: Exception) {}
                }
            }

            val socialCols = getExistingColumns(db, "social_posts")
            if (socialCols.isNotEmpty()) {
                if (!socialCols.contains("orchardId")) {
                    try {
                        db.execSQL("ALTER TABLE `social_posts` ADD COLUMN `orchardId` INTEGER DEFAULT NULL")
                    } catch (_: Exception) {}
                }
                if (!socialCols.contains("isAlertResolved")) {
                    try {
                        db.execSQL("ALTER TABLE `social_posts` ADD COLUMN `isAlertResolved` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {}
                }
            }

            val userCols = getExistingColumns(db, "user_profile")
            if (userCols.isNotEmpty()) {
                if (!userCols.contains("titularPropios")) {
                    try {
                        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `titularPropios` TEXT NOT NULL DEFAULT 'Carlos Vicente'")
                    } catch (_: Exception) {}
                }
                if (!userCols.contains("titularVr")) {
                    try {
                        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `titularVr` TEXT NOT NULL DEFAULT 'V&R C.B.'")
                    } catch (_: Exception) {}
                }
                if (!userCols.contains("titularOtros")) {
                    try {
                        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `titularOtros` TEXT NOT NULL DEFAULT 'Otros'")
                    } catch (_: Exception) {}
                }
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_6_11 = object : androidx.room.migration.Migration(6, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_7_11 = object : androidx.room.migration.Migration(7, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_8_11 = object : androidx.room.migration.Migration(8, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_9_11 = object : androidx.room.migration.Migration(9, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndColumns(db)
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "vr_agro_database"
            )
                .addMigrations(
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_6_11,
                    MIGRATION_7_11,
                    MIGRATION_8_11,
                    MIGRATION_9_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15
                )
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = try {
                    val instance = buildDatabase(context)
                    // Trigger database initialization safely
                    instance.openHelper.writableDatabase
                    instance
                } catch (e: Exception) {
                    try {
                        context.applicationContext.deleteDatabase("vr_agro_database")
                    } catch (_: Exception) {}
                    buildDatabase(context)
                }
                INSTANCE = db
                db
            }
        }

        suspend fun checkAndPopulateIfEmpty(db: AppDatabase) {
            try {
                if (db.userProfileDao().getProfileDirect() == null) {
                    db.userProfileDao().updateProfile(UserProfileEntity())
                }
                if (db.notificationSettingsDao().getSettingsDirect() == null) {
                    db.notificationSettingsDao().updateSettings(NotificationSettingsEntity())
                }
                // Limpieza automática de datos ficticios previos si existían
                cleanupMockData(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun cleanupMockData(db: AppDatabase) {
            try {
                val mockOrchardNames = listOf(
                    "Huerto El Realón",
                    "Huerto San Jaime",
                    "Huerto El Carmen",
                    "Huerto La Mina"
                )
                for (name in mockOrchardNames) {
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM work_parts WHERE orchardName = ?",
                        arrayOf(name)
                    )
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM calendar_tasks WHERE orchardName = ?",
                        arrayOf(name)
                    )
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM orchards WHERE name = ?",
                        arrayOf(name)
                    )
                }
                // Eliminar posts ficticios de bienvenida/prueba
                db.openHelper.writableDatabase.execSQL(
                    "DELETE FROM social_posts WHERE authorName = 'Carlos Vicente' AND content LIKE '%campaña de riego%'"
                )
                db.openHelper.writableDatabase.execSQL(
                    "DELETE FROM work_parts WHERE taskName IN ('Riego y comprobación goteros', 'Abonado y fertirrigación cítricos', 'Reparación y mantenimiento goteo')"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            try {
                if (db.notificationSettingsDao().getSettingsDirect() == null) {
                    db.notificationSettingsDao().updateSettings(NotificationSettingsEntity())
                }
                if (db.userProfileDao().getProfileDirect() == null) {
                    db.userProfileDao().updateProfile(UserProfileEntity())
                }
                cleanupMockData(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
