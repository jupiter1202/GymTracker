package de.jupiter1202.gymtracker.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModel(private val repository: ExerciseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup.asStateFlow()

    val exercises: StateFlow<List<Exercise>> = combine(
        _searchQuery,
        _selectedMuscleGroup
    ) { q, mg ->
        Pair(q, mg)
    }.flatMapLatest { (query, muscleGroup) ->
        repository.searchExercises(query, muscleGroup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groupedExercises: StateFlow<Map<String, List<Exercise>>> = exercises
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list.groupBy { it.primaryMuscleGroup }
            else emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _deleteResult = MutableStateFlow<DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteResult?> = _deleteResult.asStateFlow()

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onMuscleGroupSelected(group: String?) {
        _selectedMuscleGroup.value = group
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun saveExercise(name: String, muscleGroup: String, equipmentType: String, existing: Exercise?) {
        viewModelScope.launch {
            if (existing == null) {
                repository.insertExercise(
                    Exercise(
                        name = name,
                        primaryMuscleGroup = muscleGroup,
                        equipmentType = equipmentType,
                        isCustom = true
                    )
                )
            } else {
                repository.updateExercise(
                    existing.copy(
                        name = name,
                        primaryMuscleGroup = muscleGroup,
                        equipmentType = equipmentType
                    )
                )
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            _deleteResult.value = repository.deleteExercise(exercise)
        }
    }
}
