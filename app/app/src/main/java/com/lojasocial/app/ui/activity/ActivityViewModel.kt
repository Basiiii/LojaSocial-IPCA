package com.lojasocial.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.data.model.Activity
import com.lojasocial.app.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing recent activity state and business logic.
 * 
 * This ViewModel handles loading activities for both beneficiaries and employees,
 * managing loading states and error handling.
 * 
 * @param activityRepository Repository for accessing activity data
 */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Loads recent activities for a beneficiary user.
     * 
     * @param limit Maximum number of activities to load (default: 20)
     */
    fun loadActivitiesForBeneficiary(limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            activityRepository.getRecentActivitiesForBeneficiary(limit)
                .catch { exception ->
                    _error.value = exception.message ?: "Erro ao carregar atividades"
                    _isLoading.value = false
                }
                .collect { activityList ->
                    _activities.value = activityList
                    _isLoading.value = false
                }
        }
    }

    /**
     * Loads recent activities for an employee/admin user.
     * 
     * @param limit Maximum number of activities to load (default: 20)
     */
    fun loadActivitiesForEmployee(limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            activityRepository.getRecentActivitiesForEmployee(limit)
                .catch { exception ->
                    _error.value = exception.message ?: "Erro ao carregar atividades"
                    _isLoading.value = false
                }
                .collect { activityList ->
                    _activities.value = activityList
                    _isLoading.value = false
                }
        }
    }

    /**
     * Refreshes the activities list.
     */
    fun refresh(isEmployee: Boolean, limit: Int = 20) {
        if (isEmployee) {
            loadActivitiesForEmployee(limit)
        } else {
            loadActivitiesForBeneficiary(limit)
        }
    }
}
