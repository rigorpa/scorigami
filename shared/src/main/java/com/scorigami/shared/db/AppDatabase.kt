package com.scorigami.shared.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.ObDao
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
        ScoreEntity::class,
        ObEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun playerDao(): PlayerDao
    abstract fun roundDao(): RoundDao
    abstract fun scoreDao(): ScoreDao
    abstract fun obDao(): ObDao

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
        // The DDL must match Room's generated schema for ObEntity exactly (column types,
        // NOT NULL, PK order, FK actions, index name) or Room fails schema validation on open.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ob_counts` (" +
                        "`roundId` INTEGER NOT NULL, " +
                        "`playerId` INTEGER NOT NULL, " +
                        "`holeNumber` INTEGER NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`roundId`, `playerId`, `holeNumber`), " +
                        "FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ob_counts_roundId` ON `ob_counts` (`roundId`)"
                )
            }
        }
    }
}
