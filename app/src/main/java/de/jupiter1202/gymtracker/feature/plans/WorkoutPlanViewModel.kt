package de.jupiter1202.gymtracker.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseWithExercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanViewModel(
    private val repository: WorkoutPlanRepository
) : ViewModel() {

    // --- Plans list ---
    val plans: StateFlow<List<WorkoutPlan>> = repository.getPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Templates ---
    private val _templates = MutableStateFlow<List<TemplateProgram>>(emptyList())
    val templates: StateFlow<List<TemplateProgram>> = _templates.asStateFlow()

    init {
        viewModelScope.launch {
            _templates.value = repository.loadTemplates()
        }
    }

    // --- Plan detail (exercises for a specific plan) ---
    private val _activePlanId = MutableStateFlow<Long?>(null)

    val planExercises: StateFlow<List<PlanExerciseWithExercise>> =
        _activePlanId.flatMapLatest { planId ->
            if (planId != null) repository.getPlanExercises(planId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActivePlan(planId: Long) {
        _activePlanId.value = planId
    }

    // --- Plan CRUD ---
    fun createPlan(name: String, description: String?, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlan(name, description)
            onCreated(id)
        }
    }

    fun updatePlan(plan: WorkoutPlan) {
        viewModelScope.launch { repository.updatePlan(plan) }
    }

    fun deletePlan(plan: WorkoutPlan) {
        viewModelScope.launch { repository.deletePlan(plan) }
    }

    // --- Exercise management ---
    fun addExercise(planId: Long, exerciseId: Long, sets: Int, reps: String) {
        viewModelScope.launch { repository.addExercise(planId, exerciseId, sets, reps) }
    }

    fun updateExerciseTargets(planExercise: PlanExercise, sets: Int, reps: String) {
        viewModelScope.launch { repository.updateExerciseTargets(planExercise, sets, reps) }
    }

    fun removeExercise(planExercise: PlanExercise) {
        viewModelScope.launch { repository.removeExercise(planExercise) }
    }

    fun reorderExercises(exercises: List<PlanExercise>) {
        viewModelScope.launch { repository.reorderExercises(exercises) }
    }

    // --- Template import ---
    fun importTemplate(
        program: TemplateProgram,
        exerciseLookup: Map<String, Long>,
        onImported: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val planId = repository.importTemplate(program, exerciseLookup)
            onImported(planId)
        }
    }
}
