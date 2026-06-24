package com.scorigami.shared.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.scorigami.shared.db.AppDatabase
import com.scorigami.shared.db.DatabaseSeeder
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.PlayerDao
import com.scorigami.shared.db.dao.RoundDao
import com.scorigami.shared.db.dao.ScoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        courseDaoProvider: Provider<CourseDao>
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "scorigami.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .addCallback(object : RoomDatabase.Callback() {
                // Seed the pre-loaded courses exactly once, when the DB file is first created.
                // (Previously launched on every app start via an inline scope that was never
                // cancelled.) Run off the creation thread since DAO access opens its own
                // connection; the scope is bounded to this one-shot task and cancelled when done.
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    val courseDao = courseDaoProvider.get()
                    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                    scope.launch {
                        try {
                            DatabaseSeeder.seedIfEmpty(courseDao)
                        } finally {
                            scope.cancel()
                        }
                    }
                }
                // Room 2.8 removed Builder.setForeignKeyConstraintsEnabled(); enable FK enforcement
                // via the PRAGMA on every connection open (outside a transaction, so it takes
                // effect). Required for the declared onDelete = CASCADE constraints to fire.
                override fun onOpen(connection: SQLiteConnection) {
                    connection.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()
    }

    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()
    @Provides fun provideRoundDao(db: AppDatabase): RoundDao = db.roundDao()
    @Provides fun provideScoreDao(db: AppDatabase): ScoreDao = db.scoreDao()
}
