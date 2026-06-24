package com.scorigami.shared.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import com.scorigami.shared.db.entity.*

@Database(
    entities = [
        CourseEntity::class,
        HoleEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        RoundPlayerEntity::class,
        ScoreEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun playerDao(): PlayerDao
    abstract fun roundDao(): RoundDao
    abstract fun scoreDao(): ScoreDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE holes ADD COLUMN distanceMeters INTEGER")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE holes RENAME COLUMN distanceMeters TO distanceFeet")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE holes ADD COLUMN notes TEXT")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE players ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
