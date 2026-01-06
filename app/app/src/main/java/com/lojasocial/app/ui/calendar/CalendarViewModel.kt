package com.lojasocial.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.PickupRequest
import com.lojasocial.app.repository.CalendarRepository
import com.lojasocial.app.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Calendar screen.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()
    
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()
    
    private val _isEmployee = MutableStateFlow(false)
    
    private val _pickupRequests = MutableStateFlow<List<PickupRequest>>(emptyList())
    val pickupRequests: StateFlow<List<PickupRequest>> = _pickupRequests.asStateFlow()
    
    private val _pickupCounts = MutableStateFlow<Map<Date, Int>>(emptyMap())
    val pickupCounts: StateFlow<Map<Date, Int>> = _pickupCounts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Determine if user is employee
        viewModelScope.launch {
            userRepository.getCurrentUserProfile().collect { profile ->
                _isEmployee.value = profile?.isAdmin == true
                // Load initial month data
                loadMonthData()
            }
        }
    }
    
    /**
     * Selects a date and loads pickup requests for that date.
     */
    fun selectDate(date: Date) {
        _selectedDate.value = date
        loadPickupRequestsForDate(date)
    }
    
    /**
     * Navigates to the previous month.
     */
    fun navigateToPreviousMonth() {
        val calendar = Calendar.getInstance().apply {
            time = _currentMonth.value.time
            add(Calendar.MONTH, -1)
        }
        _currentMonth.value = calendar
        loadMonthData()
    }
    
    /**
     * Navigates to the next month.
     */
    fun navigateToNextMonth() {
        val calendar = Calendar.getInstance().apply {
            time = _currentMonth.value.time
            add(Calendar.MONTH, 1)
        }
        _currentMonth.value = calendar
        loadMonthData()
    }
    
    /**
     * Navigates to today's month.
     */
    fun navigateToToday() {
        _currentMonth.value = Calendar.getInstance()
        loadMonthData()
    }
    
    /**
     * Loads pickup requests for the selected date.
     */
    private fun loadPickupRequestsForDate(date: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            calendarRepository.getPickupRequestsForDate(date, _isEmployee.value)
                .collect { requests ->
                    _pickupRequests.value = requests
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Loads pickup counts for the current month.
     */
    private fun loadMonthData() {
        viewModelScope.launch {
            val calendar = _currentMonth.value
            val startDate = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val endDate = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
            
            calendarRepository.getPickupCountsForDateRange(startDate, endDate, _isEmployee.value)
                .collect { counts ->
                    _pickupCounts.value = counts
                }
        }
    }
}

