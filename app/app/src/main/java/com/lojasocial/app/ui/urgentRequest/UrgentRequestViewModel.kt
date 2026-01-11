package com.lojasocial.app.ui.urgentRequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.RequestItem
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.request.ItemsRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UrgentRequestViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val requestsRepository: RequestsRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<UrgentRequestUiState>(UrgentRequestUiState.Loading)
    val uiState: StateFlow<UrgentRequestUiState> = _uiState.asStateFlow()

    private val _productQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val productQuantities: StateFlow<Map<String, Int>> = _productQuantities.asStateFlow()

    private val _beneficiaries = MutableStateFlow<List<UserProfile>>(emptyList())
    val beneficiaries: StateFlow<List<UserProfile>> = _beneficiaries.asStateFlow()

    private val _selectedBeneficiary = MutableStateFlow<UserProfile?>(null)
    val selectedBeneficiary: StateFlow<UserProfile?> = _selectedBeneficiary.asStateFlow()

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _createdRequestId = MutableStateFlow<String?>(null)
    val createdRequestId: StateFlow<String?> = _createdRequestId.asStateFlow()

    private val _showCreateUserDialog = MutableStateFlow(false)
    val showCreateUserDialog: StateFlow<Boolean> = _showCreateUserDialog.asStateFlow()

    private var lastVisibleId: String? = null
    private var isLoading = false
    private var isEndOfList = false

    init {
        fetchProducts()
        fetchBeneficiaries()
    }

    fun fetchProducts(isLoadMore: Boolean = false) {
        if (isLoading || (isLoadMore && isEndOfList)) return

        viewModelScope.launch {
            isLoading = true
            if (!isLoadMore) {
                _uiState.value = UrgentRequestUiState.Loading
            }

            try {
                val newProducts = itemsRepository.getProducts(lastVisibleId = lastVisibleId)

                if (newProducts.isNotEmpty()) {
                    lastVisibleId = newProducts.last().docId

                    val currentProducts = if (isLoadMore && _uiState.value is UrgentRequestUiState.Success) {
                        (_uiState.value as UrgentRequestUiState.Success).products
                    } else {
                        emptyList()
                    }

                    _uiState.value = UrgentRequestUiState.Success(currentProducts + newProducts)
                } else if (!isLoadMore) {
                    _uiState.value = UrgentRequestUiState.Success(emptyList())
                } else {
                    isEndOfList = true
                }

            } catch (e: Exception) {
                if (!isLoadMore) {
                    _uiState.value = UrgentRequestUiState.Error
                }
            }
            isLoading = false
        }
    }

    fun fetchBeneficiaries() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { usersList ->
                _beneficiaries.value = usersList
            }
        }
    }

    fun onAddProduct(productDocId: String) {
        val currentQuantity = _productQuantities.value[productDocId] ?: 0
        val availableStock = getAvailableStock(productDocId)
        if (currentQuantity >= availableStock) {
            return
        }
        val newMap = _productQuantities.value.toMutableMap()
        newMap[productDocId] = currentQuantity + 1
        _productQuantities.value = newMap
    }

    private fun getAvailableStock(productDocId: String): Int {
        val products = when (val state = _uiState.value) {
            is UrgentRequestUiState.Success -> state.products
            else -> emptyList()
        }
        return products.find { it.docId == productDocId }?.stock ?: 0
    }

    fun onRemoveProduct(productDocId: String) {
        val currentQuantity = _productQuantities.value[productDocId] ?: 0
        if (currentQuantity > 0) {
            val newMap = _productQuantities.value.toMutableMap()
            if (currentQuantity == 1) {
                newMap.remove(productDocId)
            } else {
                newMap[productDocId] = currentQuantity - 1
            }
            _productQuantities.value = newMap
        }
    }

    fun selectBeneficiary(beneficiary: UserProfile) {
        _selectedBeneficiary.value = beneficiary
    }

    fun showCreateUserDialog() {
        _showCreateUserDialog.value = true
    }

    fun hideCreateUserDialog() {
        _showCreateUserDialog.value = false
    }

    fun createUserAndSelect(name: String, email: String) {
        viewModelScope.launch {
            try {
                // Generate a unique document ID for the user (without creating in Firebase Auth)
                val newUserId = firestore.collection("users").document().id
                
                val newUser = UserProfile(
                    uid = newUserId,
                    email = email,
                    name = name,
                    isAdmin = false,
                    isBeneficiary = false
                )
                
                // Create profile in Firestore only (no Firebase Auth account)
                val profileResult = userRepository.createProfile(newUser)
                profileResult.fold(
                    onSuccess = {
                        // Add to beneficiaries list and select
                        _beneficiaries.value = _beneficiaries.value + newUser
                        _selectedBeneficiary.value = newUser
                        _showCreateUserDialog.value = false
                    },
                    onFailure = { error ->
                        _submissionState.value = SubmissionState.Error("Erro ao criar perfil: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error("Erro: ${e.message}")
            }
        }
    }

    fun submitUrgentRequest() {
        val itemsToSend = _productQuantities.value
        val beneficiary = _selectedBeneficiary.value

        if (itemsToSend.isEmpty()) {
            _submissionState.value = SubmissionState.Error("Por favor, selecione pelo menos um item")
            return
        }

        if (beneficiary == null) {
            _submissionState.value = SubmissionState.Error("Por favor, selecione um beneficiário")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            val result = requestsRepository.createUrgentRequest(beneficiary.uid, itemsToSend)
            result.fold(
                onSuccess = { requestId ->
                    _productQuantities.value = emptyMap()
                    _selectedBeneficiary.value = null
                    _createdRequestId.value = requestId
                    _submissionState.value = SubmissionState.Success
                },
                onFailure = { error ->
                    _submissionState.value = SubmissionState.Error(error.message ?: "Erro desconhecido")
                }
            )
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = SubmissionState.Idle
    }
}

sealed class UrgentRequestUiState {
    object Loading : UrgentRequestUiState()
    data class Success(val products: List<RequestItem>) : UrgentRequestUiState()
    object Error : UrgentRequestUiState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}
