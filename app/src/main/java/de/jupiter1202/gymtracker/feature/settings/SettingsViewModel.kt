package de.jupiter1202.gymtracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val weightUnit: StateFlow<String> = repository.weightUnit.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "kg"
    )

    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            repository.setWeightUnit(unit)
        }
    }
}
