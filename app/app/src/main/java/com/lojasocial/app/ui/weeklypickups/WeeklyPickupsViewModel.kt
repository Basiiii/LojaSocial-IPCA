package com.lojasocial.app.ui.weeklypickups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.repository.user.ProfilePictureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WeeklyPickupsViewModel @Inject constructor(
    private val requestsRepository: RequestsRepository,
    private val authRepository: AuthRepository,
    private val profilePictureRepository: ProfilePictureRepository,
    val productRepository: com.lojasocial.app.repository.product.ProductRepository
) : ViewModel() {

    private val _weeklyPickups = MutableStateFlow<List<Request>>(emptyList())
    val weeklyPickups: StateFlow<List<Request>> = _weeklyPickups.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _beneficiaryProfiles = MutableStateFlow<Map<String, UserProfileData>>(emptyMap())
    val beneficiaryProfiles: StateFlow<Map<String, UserProfileData>> = _beneficiaryProfiles.asStateFlow()

    private val _profilePictures = MutableStateFlow<Map<String, String?>>(emptyMap())
    val profilePictures: StateFlow<Map<String, String?>> = _profilePictures.asStateFlow()

    private val _selectedRequest = MutableStateFlow<Request?>(null)
    val selectedRequest: StateFlow<Request?> = _selectedRequest.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()

    private val _isLoadingRequest = MutableStateFlow(false)
    val isLoadingRequest: StateFlow<Boolean> = _isLoadingRequest.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            _currentUserId.value = currentUser?.uid
            loadWeeklyPickups()
        }
    }

    private fun loadWeeklyPickups() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                requestsRepository.getAllRequests()
                    .collect { allRequests ->
                        // Calculate current week range
                        val calendar = Calendar.getInstance()
                        val today = calendar.time

                        // Get start of current week (Monday)
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startOfWeek = calendar.time

                        // Get end of current week (Sunday)
                        calendar.add(Calendar.DAY_OF_WEEK, 6)
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        val endOfWeek = calendar.time

                        // Filter requests scheduled for this week (status 1 or 2 with scheduledPickupDate)
                        val weeklyRequests = allRequests.filter { request ->
                            (request.status == 1 || request.status == 2) &&
                            request.scheduledPickupDate != null &&
                            request.scheduledPickupDate!! >= startOfWeek &&
                            request.scheduledPickupDate!! <= endOfWeek
                        }.sortedBy { it.scheduledPickupDate }

                        _weeklyPickups.value = weeklyRequests

                        // Fetch beneficiary profiles and profile pictures
                        val profilesMap = coroutineScope {
                            weeklyRequests.map { request ->
                                async {
                                    val userResult = requestsRepository.getUserProfile(request.userId)
                                    request.id to userResult.getOrElse { UserProfileData() }
                                }
                            }.awaitAll().toMap()
                        }

                        val picturesMap = coroutineScope {
                            weeklyRequests.map { request ->
                                async {
                                    try {
                                        request.id to profilePictureRepository.getProfilePicture(request.userId).firstOrNull()
                                    } catch (e: Exception) {
                                        request.id to null
                                    }
                                }
                            }.awaitAll().toMap()
                        }

                        _beneficiaryProfiles.value = profilesMap
                        _profilePictures.value = picturesMap
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun selectRequest(requestId: String) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.getRequestById(requestId)
            result.fold(
                onSuccess = { request ->
                    _selectedRequest.value = request
                    val userResult = requestsRepository.getUserProfile(request.userId)
                    userResult.fold(
                        onSuccess = { profile ->
                            _userProfile.value = profile
                        },
                        onFailure = {
                            _userProfile.value = UserProfileData()
                        }
                    )
                    _isLoadingRequest.value = false
                },
                onFailure = { error ->
                    _isLoadingRequest.value = false
                }
            )
        }
    }

    fun clearSelectedRequest() {
        _selectedRequest.value = null
        _userProfile.value = null
    }

    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.completeRequest(requestId)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    loadWeeklyPickups()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }

    fun cancelDelivery(requestId: String, beneficiaryAbsent: Boolean = false) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.cancelDelivery(requestId, beneficiaryAbsent)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    loadWeeklyPickups()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }

    fun rescheduleDelivery(requestId: String, newDate: Date, isEmployeeRescheduling: Boolean) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.rescheduleDelivery(requestId, newDate, isEmployeeRescheduling)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    loadWeeklyPickups()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }
}
