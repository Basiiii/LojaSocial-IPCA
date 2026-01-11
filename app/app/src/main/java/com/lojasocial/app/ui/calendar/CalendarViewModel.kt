package com.lojasocial.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.request.PickupRequest
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.calendar.CalendarRepository
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.repository.user.UserRepository
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

/**
 * ViewModel for the Calendar screen.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val userRepository: UserRepository,
    private val campaignRepository: CampaignRepository,
    private val requestsRepository: RequestsRepository,
    private val authRepository: AuthRepository,
    private val profilePictureRepository: ProfilePictureRepository
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()
    
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()
    
    private val _isEmployee = MutableStateFlow(false)
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    private val _isBeneficiary = MutableStateFlow(false)
    private val _isBeneficiaryPortal = MutableStateFlow(false)
    
    private val _pickupRequests = MutableStateFlow<List<PickupRequest>>(emptyList())
    val pickupRequests: StateFlow<List<PickupRequest>> = _pickupRequests.asStateFlow()
    
    private val _pickupCounts = MutableStateFlow<Map<Date, Int>>(emptyMap())
    val pickupCounts: StateFlow<Map<Date, Int>> = _pickupCounts.asStateFlow()
    
    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()
    
    // Map of date to list of campaigns for that date
    private val _campaignsByDate = MutableStateFlow<Map<Date, List<Campaign>>>(emptyMap())
    val campaignsByDate: StateFlow<Map<Date, List<Campaign>>> = _campaignsByDate.asStateFlow()
    
    // Map of date to list of accepted requests (Pedido Aceite) for that date
    private val _acceptedRequestsByDate = MutableStateFlow<Map<Date, List<Request>>>(emptyMap())
    val acceptedRequestsByDate: StateFlow<Map<Date, List<Request>>> = _acceptedRequestsByDate.asStateFlow()
    
    // Map of request ID to beneficiary profile data (name and profile picture)
    private val _beneficiaryProfiles = MutableStateFlow<Map<String, UserProfileData>>(emptyMap())
    val beneficiaryProfiles: StateFlow<Map<String, UserProfileData>> = _beneficiaryProfiles.asStateFlow()
    
    // Map of request ID to profile picture base64
    private val _profilePictures = MutableStateFlow<Map<String, String?>>(emptyMap())
    val profilePictures: StateFlow<Map<String, String?>> = _profilePictures.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Request selection for dialog
    private val _selectedRequest = MutableStateFlow<Request?>(null)
    val selectedRequest: StateFlow<Request?> = _selectedRequest.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()
    
    private val _isLoadingRequest = MutableStateFlow(false)
    val isLoadingRequest: StateFlow<Boolean> = _isLoadingRequest.asStateFlow()
    
    init {
        // Determine if user is employee and get current user ID
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            _currentUserId.value = currentUser?.uid
            
            try {
                userRepository.getCurrentUserProfile()
                    .catch { e ->
                        // Handle Firestore errors gracefully (e.g., permission denied after logout)
                        _isEmployee.value = false
                        _isBeneficiary.value = false
                    }
                    .collect { profile ->
                        // Only update if we still have a valid user
                        if (authRepository.getCurrentUser() != null) {
                            _isEmployee.value = profile?.isAdmin == true
                            _isBeneficiary.value = profile?.isBeneficiary == true
                            // Load initial month data
                            loadMonthData()
                            // Load campaigns
                            loadCampaigns()
                            // Load accepted requests
                            loadAcceptedRequests()
                        } else {
                            _isEmployee.value = false
                            _isBeneficiary.value = false
                        }
                    }
            } catch (e: Exception) {
                // Handle any other errors
                _isEmployee.value = false
                _isBeneficiary.value = false
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
    
    /**
     * Loads all campaigns and creates a map of dates to campaigns.
     */
    private fun loadCampaigns() {
        viewModelScope.launch {
            try {
                val allCampaigns = campaignRepository.getAllCampaigns()
                _campaigns.value = allCampaigns
                
                // Create a map of date to list of campaigns for that date
                val campaignsMap = mutableMapOf<Date, MutableList<Campaign>>()
                
                allCampaigns.forEach { campaign ->
                    val startCal = Calendar.getInstance().apply {
                        time = campaign.startDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val endCal = Calendar.getInstance().apply {
                        time = campaign.endDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    // Add campaign to all dates between start and end (inclusive)
                    val currentDate = Calendar.getInstance().apply { 
                        time = startCal.time 
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val endDateNormalized = endCal.timeInMillis
                    while (currentDate.timeInMillis <= endDateNormalized) {
                        val dateKey = Calendar.getInstance().apply {
                            time = currentDate.time
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                        
                        campaignsMap.getOrPut(dateKey) { mutableListOf() }.add(campaign)
                        currentDate.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                
                _campaignsByDate.value = campaignsMap
            } catch (e: Exception) {
                // Handle error silently or log it
                _campaigns.value = emptyList()
                _campaignsByDate.value = emptyMap()
            }
        }
    }
    
    /**
     * Loads all accepted requests (status = 1, PENDENTE_LEVANTAMENTO) and concluded requests (status = 2, CONCLUIDO)
     * and creates a map of dates to requests.
     * Employees see all accepted requests, beneficiaries only see their own.
     */
    private fun loadAcceptedRequests() {
        viewModelScope.launch {
            try {
                requestsRepository.getAllRequests()
                    .collect { allRequests ->
                        val currentUserId = _currentUserId.value
                        val isEmployee = _isEmployee.value
                        val isBeneficiaryPortal = _isBeneficiaryPortal.value
                        
                        // Filter accepted requests (status = 1) and concluded requests (status = 2) with scheduledPickupDate
                        var acceptedRequests = allRequests.filter { 
                            (it.status == 1 || it.status == 2) && it.scheduledPickupDate != null 
                        }
                        
                        // If accessing from beneficiary portal, filter to only show user's own requests
                        // This applies even if the user is also an admin
                        if (isBeneficiaryPortal && currentUserId != null) {
                            acceptedRequests = acceptedRequests.filter { request ->
                                request.userId == currentUserId
                            }
                        } else if (!isEmployee && currentUserId != null) {
                            // If not an employee and not in beneficiary portal, filter to only show their own requests
                            acceptedRequests = acceptedRequests.filter { request ->
                                request.userId == currentUserId
                            }
                        }
                        // If user is an employee accessing from employee portal, show all accepted requests
                        
                        // Create a map of date to list of accepted requests for that date
                        val requestsMap = mutableMapOf<Date, MutableList<Request>>()
                        
                        acceptedRequests.forEach { request ->
                            request.scheduledPickupDate?.let { pickupDate ->
                                val dateKey = Calendar.getInstance().apply {
                                    time = pickupDate
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.time
                                
                                requestsMap.getOrPut(dateKey) { mutableListOf() }.add(request)
                            }
                        }
                        
                        _acceptedRequestsByDate.value = requestsMap
                        
                        // Fetch beneficiary profiles for all accepted requests in parallel
                        val profilesMap = coroutineScope {
                            acceptedRequests.map { request ->
                                async {
                                    val userResult = requestsRepository.getUserProfile(request.userId)
                                    request.id to userResult.getOrElse { UserProfileData() }
                                }
                            }.awaitAll().toMap()
                        }
                        
                        val picturesMap = coroutineScope {
                            acceptedRequests.map { request ->
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
                    }
            } catch (e: Exception) {
                // Handle error silently or log it
                _acceptedRequestsByDate.value = emptyMap()
            }
        }
    }
    
    /**
     * Selects a request and loads its details for the dialog.
     */
    fun selectRequest(requestId: String) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.getRequestById(requestId)
            result.fold(
                onSuccess = { request ->
                    _selectedRequest.value = request
                    // Fetch user profile
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
                    // Handle error silently
                }
            )
        }
    }
    
    /**
     * Clears the selected request.
     */
    fun clearSelectedRequest() {
        _selectedRequest.value = null
        _userProfile.value = null
    }
    
    /**
     * Completes a request (status 2 = CONCLUIDO).
     */
    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.completeRequest(requestId)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    // Reload accepted requests to reflect the change
                    loadAcceptedRequests()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }
    
    /**
     * Cancels a delivery (status 3 = CANCELADO).
     */
    fun cancelDelivery(requestId: String, beneficiaryAbsent: Boolean = false) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.cancelDelivery(requestId, beneficiaryAbsent)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    // Reload accepted requests to reflect the change
                    loadAcceptedRequests()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }
    
    /**
     * Sets whether the calendar is being accessed from the beneficiary portal.
     * This affects filtering of accepted requests.
     */
    fun setBeneficiaryPortalContext(isBeneficiaryPortal: Boolean) {
        _isBeneficiaryPortal.value = isBeneficiaryPortal
        // Reload accepted requests with new context
        loadAcceptedRequests()
    }
    
    /**
     * Reschedules a delivery (changes status from PENDENTE_LEVANTAMENTO back to SUBMETIDO).
     */
    fun rescheduleDelivery(requestId: String, newDate: Date, isEmployeeRescheduling: Boolean) {
        viewModelScope.launch {
            _isLoadingRequest.value = true
            val result = requestsRepository.rescheduleDelivery(requestId, newDate, isEmployeeRescheduling)
            result.fold(
                onSuccess = {
                    _isLoadingRequest.value = false
                    clearSelectedRequest()
                    // Reload accepted requests to reflect the change
                    loadAcceptedRequests()
                },
                onFailure = {
                    _isLoadingRequest.value = false
                }
            )
        }
    }
}

