package com.scorigami.shared.db

import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.entity.CourseEntity
import com.scorigami.shared.db.entity.HoleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSeeder {

    suspend fun seedIfEmpty(courseDao: CourseDao) = withContext(Dispatchers.IO) {
        if (courseDao.getCourseCount() > 0) return@withContext
        seedColomos(courseDao)
        seedCentinela(courseDao)
    }

    private suspend fun seedColomos(courseDao: CourseDao) {
        val courseId = courseDao.insertCourse(CourseEntity(name = "Los Colomos", holeCount = 18))
        val pars      = listOf(3, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 3, 3, 3)
        val distances = listOf(199, 326, 185, 247, 299, 230, 269, 201, 350, 203, 195, 211, 399, 247, 201, 225, 296, 323)
        // Add hole-specific rules here — use null for holes with no special rules
        val notes = listOf(
            null,  // Hole 1
            "OB road to the left and beyond.",  // Hole 2
            "OB half-circle area on Teepad #17",  // Hole 3
            null,  // Hole 4
            null,  // Hole 5
            "OB the beyond the far-side path",  // Hole 6
            null,  // Hole 7
            null,  // Hole 8
            "OB the path and beyond",  // Hole 9
            "OB the path and beyond",  // Hole 10
            null,  // Hole 11
            null,  // Hole 12
            "Double mando first tree to the right and large left tree 150 ft. out. Drop zone halfway mandos",  // Hole 13
            "OB path and beyond",  // Hole 14
            "OB path and beyond",  // Hole 15
            "OB canyon which shares Hole 18 putting area",  // Hole 16
            "Island hole. OB everything except semi-circle surrounded by stones",  // Hole 17
            null   // Hole 18
        )
        courseDao.insertHoles(pars.mapIndexed { i, par ->
            HoleEntity(courseId = courseId, number = i + 1, par = par, distanceFeet = distances[i], notes = notes[i])
        })
    }

    private suspend fun seedCentinela(courseDao: CourseDao) {
        val courseId  = courseDao.insertCourse(CourseEntity(name = "El Centinela", holeCount = 18))
        val distances = listOf(274, 220, 188, 191, 202, 272, 176, 202, 203, 253, 244, 229, 189, 177, 260, 203, 211, 236)
        // Add hole-specific rules here — use null for holes with no special rules
        val notes = listOf(
            null,  // Hole 1
            null,  // Hole 2
            null,  // Hole 3
            null,  // Hole 4
            null,  // Hole 5
            null,  // Hole 6
            null,  // Hole 7
            null,  // Hole 8
            null,  // Hole 9
            null,  // Hole 10
            null,  // Hole 11
            null,  // Hole 12
            null,  // Hole 13
            null,  // Hole 14
            null,  // Hole 15
            null,  // Hole 16
            null,  // Hole 17
            null   // Hole 18
        )
        courseDao.insertHoles(distances.mapIndexed { i, dist ->
            HoleEntity(courseId = courseId, number = i + 1, par = 3, distanceFeet = dist, notes = notes[i])
        })
    }
}
