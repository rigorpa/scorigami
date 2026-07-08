package com.scorigami.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scorigami.shared.db.dao.CourseDao
import com.scorigami.shared.db.dao.CourseWithHoles
import com.scorigami.shared.db.entity.CourseEntity
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.sync.SgCourse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val courses: StateFlow<List<CourseWithHoles>> = courseDao.getAllCoursesWithHoles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val editingCourseId: Long = savedStateHandle.get<Long>("courseId") ?: -1L

    /** True when opened to edit an existing course; false when creating a new one. */
    val isEditing: Boolean = editingCourseId != -1L

    val editingCourse: StateFlow<CourseWithHoles?> = flow {
        emit(if (editingCourseId == -1L) null else courseDao.getCourseWithHoles(editingCourseId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _importedCourse = MutableSharedFlow<Pair<String, Int>>(extraBufferCapacity = 1)
    val importedCourse: SharedFlow<Pair<String, Int>> = _importedCourse.asSharedFlow()

    fun saveCourse(
        name: String,
        parValues: List<Int>,
        notesValues: List<String> = emptyList(),
        distanceValues: List<Int?> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val courseId = if (editingCourseId == -1L) {
                courseDao.insertCourse(CourseEntity(name = name.trim(), holeCount = parValues.size))
            } else {
                courseDao.insertCourse(CourseEntity(id = editingCourseId, name = name.trim(), holeCount = parValues.size))
            }
            courseDao.insertHoles(parValues.mapIndexed { i, par ->
                HoleEntity(
                    courseId = courseId,
                    number = i + 1,
                    par = par,
                    distanceFeet = distanceValues.getOrNull(i),
                    notes = notesValues.getOrNull(i)?.trim()?.ifEmpty { null }
                )
            })
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) { courseDao.deleteCourse(course) }
    }

    fun importCourse(sgCourse: SgCourse) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingNames = courseDao.getAllCourseNames().toSet()
            val finalName = deduplicateName(sgCourse.name, existingNames)

            val courseId = courseDao.insertCourse(
                CourseEntity(name = finalName, holeCount = sgCourse.holes.size)
            )
            courseDao.insertHoles(sgCourse.holes.map { hole ->
                HoleEntity(
                    courseId = courseId,
                    number = hole.number,
                    par = maxOf(2, hole.par),
                    distanceFeet = hole.distanceFeet,
                    notes = hole.notes
                )
            })

            _importedCourse.tryEmit(Pair(finalName, sgCourse.holes.size))
        }
    }

    private fun deduplicateName(name: String, existingNames: Set<String>): String {
        if (name !in existingNames) return name
        var suffix = 2
        while ("$name ($suffix)" in existingNames) suffix++
        return "$name ($suffix)"
    }
}

