package com.scorigami.shared.di

import android.content.Context
import androidx.room.Room
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
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "scorigami.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            DatabaseSeeder.seedIfEmpty(db.courseDao())
        }
        return db
    }

    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()
    @Provides fun provideRoundDao(db: AppDatabase): RoundDao = db.roundDao()
    @Provides fun provideScoreDao(db: AppDatabase): ScoreDao = db.scoreDao()
}
