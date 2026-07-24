package com.scorigami.shared.db.dao

import androidx.room.*
import com.scorigami.shared.db.entity.CourseEntity
import com.scorigami.shared.db.entity.HoleEntity
import kotlinx.coroutines.flow.Flow

data class CourseWithHoles(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId", entity = HoleEntity::class)
    val holes: List<HoleEntity>
)

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCoursesWithHoles(): Flow<List<CourseWithHoles>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseWithHoles(id: Long): CourseWithHoles?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoles(holes: List<HoleEntity>)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    @Query("SELECT name FROM courses")
    suspend fun getAllCourseNames(): List<String>

    @Query("SELECT * FROM courses WHERE name = :name LIMIT 1")
    suspend fun getCourseByName(name: String): CourseEntity?
}
