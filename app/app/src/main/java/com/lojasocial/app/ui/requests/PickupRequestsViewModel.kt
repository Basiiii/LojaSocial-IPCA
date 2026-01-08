package com.lojasocial.app.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PickupRequestsViewModel @Inject constructor(
    private val repository: RequestsRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<PickupRequestsUiState>(PickupRequestsUiState.Loading)
    val uiState: StateFlow<PickupRequestsUiState> = _uiState.asStateFlow()

    private val _selectedRequest = MutableStateFlow<Request?>(null)
    val selectedRequest: StateFlow<Request?> = _selectedRequest.asStateFlow()

    private val _userProfile = MutableStateFlow<com.lojasocial.app.repository.request.UserProfileData?>(null)
    val userProfile: StateFlow<com.lojasocial.app.repository.request.UserProfileData?> = _userProfile.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        fetchRequests()
    }

    fun fetchRequests() {
        viewModelScope.launch {
            repository.getAllRequests()
                .catch { exception ->
                    _uiState.value = PickupRequestsUiState.Error(exception.message ?: "Erro ao carregar pedidos")
                }
                .collect { requests ->
                    _uiState.value = PickupRequestsUiState.Success(requests)
                }
        }
    }

    fun selectRequest(requestId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.getRequestById(requestId)
            result.fold(
                onSuccess = { request ->
                    _selectedRequest.value = request
                    // Fetch user profile
                    val userResult = repository.getUserProfile(request.userId)
                    userResult.fold(
                        onSuccess = { profile ->
                            _userProfile.value = profile
                        },
                        onFailure = {
                            _userProfile.value = com.lojasocial.app.repository.request.UserProfileData()
                        }
                    )
                    _actionState.value = ActionState.Idle
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao carregar detalhes")
                }
            )
        }
    }

    fun acceptRequest(requestId: String, scheduledPickupDate: Date) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.acceptRequest(requestId, scheduledPickupDate)
            result.fold(
                onSuccess = {
                    // Get request to send notification
                    val requestResult = repository.getRequestById(requestId)
                    requestResult.fold(
                        onSuccess = { request ->
                            // Send notification to user
                            sendAcceptanceNotification(request.userId, scheduledPickupDate)
                        },
                        onFailure = { }
                    )
                    _actionState.value = ActionState.Success("Pedido aceite com sucesso")
                    fetchRequests() // Refresh list
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao aceitar pedido")
                }
            )
        }
    }

    fun rejectRequest(requestId: String, reason: String?) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.rejectRequest(requestId, reason)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Pedido rejeitado")
                    fetchRequests() // Refresh list
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao rejeitar pedido")
                }
            )
        }
    }

    fun clearSelectedRequest() {
        _selectedRequest.value = null
        _userProfile.value = null
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    private suspend fun sendAcceptanceNotification(userId: String, pickupDate: Date) {
        val tokenResult = repository.getUserFcmToken(userId)
        tokenResult.fold(
            onSuccess = { token ->
                token?.let {
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "PT"))
                    val formattedDate = dateFormat.format(pickupDate)
                    notificationHelper.sendNotification(
                        token = it,
                        title = "Pedido Aceite",
                        body = "O teu pedido foi aceite. Data de levantamento: $formattedDate"
                    )
                }
            },
            onFailure = { }
        )
    }
}

sealed class PickupRequestsUiState {
    object Loading : PickupRequestsUiState()
    data class Success(val requests: List<Request>) : PickupRequestsUiState()
    data class Error(val message: String) : PickupRequestsUiState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}

