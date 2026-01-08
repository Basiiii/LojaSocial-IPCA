package com.lojasocial.app.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.audit.AuditLogEntry
import com.lojasocial.app.repository.audit.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AuditLogsViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogsUiState())
    val uiState: StateFlow<AuditLogsUiState> = _uiState.asStateFlow()

    // Date state
    private val _startDate = MutableStateFlow<Date?>(null)
    val startDate: StateFlow<Date?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Date?>(null)
    val endDate: StateFlow<Date?> = _endDate.asStateFlow()

    // Date picker visibility
    private val _showStartDatePicker = MutableStateFlow(false)
    val showStartDatePicker: StateFlow<Boolean> = _showStartDatePicker.asStateFlow()

    private val _showEndDatePicker = MutableStateFlow(false)
    val showEndDatePicker: StateFlow<Boolean> = _showEndDatePicker.asStateFlow()

    init {
        // Set default dates to last 7 days
        val calendar = Calendar.getInstance()
        _endDate.value = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        _startDate.value = calendar.time
    }

    fun showStartDatePicker() {
        _showStartDatePicker.value = true
    }

    fun showEndDatePicker() {
        _showEndDatePicker.value = true
    }

    fun dismissStartDatePicker() {
        _showStartDatePicker.value = false
    }

    fun dismissEndDatePicker() {
        _showEndDatePicker.value = false
    }

    fun onStartDateSelected(day: Int, month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        _startDate.value = calendar.time
        _showStartDatePicker.value = false
    }

    fun onEndDateSelected(day: Int, month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        _endDate.value = calendar.time
        _showEndDatePicker.value = false
    }

    fun fetchLogs() {
        val start = _startDate.value
        val end = _endDate.value

        if (start == null || end == null) {
            _uiState.value = _uiState.value.copy(
                error = "Por favor, selecione ambas as datas"
            )
            return
        }

        if (start.after(end)) {
            _uiState.value = _uiState.value.copy(
                error = "A data de início deve ser anterior à data de fim"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val startDateStr = formatDateForApi(start)
            val endDateStr = formatDateForApi(end)

            auditRepository.getLogs(startDateStr, endDateStr).fold(
                onSuccess = { logs ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = logs,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao carregar registos"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun formatDateForApi(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    fun formatDateForDisplay(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatTimestampForDisplay(timestamp: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timestamp)
            if (date != null) {
                val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                displayFormat.format(date)
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}

data class AuditLogsUiState(
    val isLoading: Boolean = false,
    val logs: List<AuditLogEntry> = emptyList(),
    val error: String? = null
)
