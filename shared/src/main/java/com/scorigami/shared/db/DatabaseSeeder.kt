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
        val pars = listOf(3, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 3, 3, 3)
        val distances = listOf(199, 326, 185, 247, 299, 230, 269, 201, 350, 203, 195, 211, 399, 247, 201, 225, 296, 323)
        courseDao.insertHoles(pars.mapIndexed { i, par ->
            HoleEntity(courseId = courseId, number = i + 1, par = par, distanceFeet = distances[i])
        })
    }

    private suspend fun seedCentinela(courseDao: CourseDao) {
        val courseId = courseDao.insertCourse(CourseEntity(name = "El Centinela", holeCount = 18))
        val distances = listOf(274, 220, 188, 191, 202, 272, 176, 202, 203, 253, 244, 229, 189, 177, 260, 203, 211, 236)
        courseDao.insertHoles(distances.mapIndexed { i, dist ->
            HoleEntity(courseId = courseId, number = i + 1, par = 3, distanceFeet = dist)
        })
    }
}
