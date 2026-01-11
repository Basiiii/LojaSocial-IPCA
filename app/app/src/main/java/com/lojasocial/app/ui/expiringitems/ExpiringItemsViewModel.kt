package com.lojasocial.app.ui.expiringitems

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lojasocial.app.domain.stock.ExpiringItemWithProduct
import com.lojasocial.app.domain.stock.ExpiringItemsUiState
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.product.StockItemRepository
import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lojasocial.app.ui.expiringitems.ExpiringItemsConstants
import com.lojasocial.app.ui.expiringitems.components.Institution
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Update state for expiring items operation.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * ViewModel for managing expiring items state and business logic.
 * Responsible for loading expiring items, managing state, and handling submissions.
 */
@HiltViewModel
class ExpiringItemsViewModel @Inject constructor(
    private val stockItemRepository: StockItemRepository,
    private val productRepository: ProductRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiringItemsUiState(isLoading = true))
    val uiState: StateFlow<ExpiringItemsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ExpiringItemsConstants.DEFAULT_FILTER)
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val selectedQuantities: StateFlow<Map<String, Int>> = _selectedQuantities.asStateFlow()

    private val _institutionName = MutableStateFlow("")
    val institutionName: StateFlow<String> = _institutionName.asStateFlow()

    private val _institutions = MutableStateFlow<List<Institution>>(emptyList())
    val institutions: StateFlow<List<Institution>> = _institutions.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _hasMoreItems = MutableStateFlow(true)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _lastLoadedExpirationDate = MutableStateFlow<java.util.Date?>(null)
    val lastLoadedExpirationDate: StateFlow<java.util.Date?> = _lastLoadedExpirationDate.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        try {
            initializeFlows()
            loadExpiringItems()
            loadInstitutions()
        } catch (e: Exception) {
            Log.e("ExpiringItemsViewModel", "Error during ViewModel initialization", e)
            _uiState.value = ExpiringItemsUiState(
                isLoading = false,
                error = "Erro na inicialização: ${e.message}"
            )
        }
    }

    private fun initializeFlows() {
        _uiState.value = ExpiringItemsUiState(isLoading = true)
        _selectedFilter.value = ExpiringItemsConstants.DEFAULT_FILTER
        _selectedQuantities.value = emptyMap()
        _institutionName.value = ""
        _institutions.value = emptyList()
        _updateState.value = UpdateState.Idle
        _hasMoreItems.value = true
        _isLoadingMore.value = false
        _lastLoadedExpirationDate.value = null
    }

    fun setSelectedFilter(filter: String) {
        try {
            _selectedFilter.value = filter
            resetPagination()
            applyFilterToItems()
        } catch (e: Exception) {
            Log.e("ExpiringItemsViewModel", "Error setting selected filter", e)
        }
    }

    fun setInstitutionName(name: String) {
        try {
            _institutionName.value = name
        } catch (e: Exception) {
            Log.e("ExpiringItemsViewModel", "Error setting institution name", e)
        }
    }

    fun loadMoreExpiringItems() {
        viewModelScope.launch {
            try {
                if (_isLoadingMore.value || !_hasMoreItems.value) return@launch

                _isLoadingMore.value = true
                val lastDate = _lastLoadedExpirationDate.value
                val (newStockItems, hasMore) = stockItemRepository.getExpiringItemsPaginated(
                    daysThreshold = 30,
                    limit = 5,
                    lastExpirationDate = lastDate
                )

                if (newStockItems.isEmpty()) {
                    _hasMoreItems.value = false
                    _isLoadingMore.value = false
                    return@launch
                }

                val newItemsWithProducts = newStockItems.map { stockItem ->
                    val product = if (stockItem.barcode.isNotEmpty()) {
                        productRepository.getProductByBarcodeId(stockItem.barcode)
                    } else {
                        null
                    }

                    val daysUntilExpiration = stockItem.expirationDate?.let { expDate ->
                        val now = Calendar.getInstance()
                        val expiration = Calendar.getInstance().apply {
                            time = expDate
                        }
                        val diffInMillis = expiration.timeInMillis - now.timeInMillis
                        (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                    } ?: 0

                    ExpiringItemWithProduct(
                        stockItem = stockItem,
                        product = product,
                        daysUntilExpiration = daysUntilExpiration
                    )
                }

                val currentAllItems = _uiState.value.allItems ?: emptyList()
                val currentIds = currentAllItems.map { it.stockItem.id }.toSet()
                val uniqueNewItems = newItemsWithProducts.filter { it.stockItem.id !in currentIds }

                if (uniqueNewItems.isEmpty()) {
                    _hasMoreItems.value = false
                    _isLoadingMore.value = false
                    return@launch
                }

                val updatedAllItems = currentAllItems + uniqueNewItems
                val sortedItems = updatedAllItems.sortedBy { it.daysUntilExpiration }

                _hasMoreItems.value = hasMore
                _lastLoadedExpirationDate.value = uniqueNewItems.lastOrNull()?.stockItem?.expirationDate ?: lastDate

                _uiState.value = _uiState.value.copy(
                    allItems = sortedItems,
                    items = applyFilterToItemsList(sortedItems, _selectedFilter.value)
                )

                Log.d("ExpiringItemsViewModel", "Loaded ${uniqueNewItems.size} new unique items. Has more: $hasMore")
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error loading more expiring items", e)
                _hasMoreItems.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun resetPagination() {
        try {
            _hasMoreItems.value = true
            _lastLoadedExpirationDate.value = null
            _isLoadingMore.value = false
        } catch (e: Exception) {
            Log.e("ExpiringItemsViewModel", "Error resetting pagination", e)
        }
    }

    private fun loadInstitutions() {
        viewModelScope.launch {
            try {
                firestore.collection("institution")
                    .orderBy("lastPickup", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { result ->
                        val institutionsList = result.documents.mapNotNull { document ->
                            val name = document.getString("name")
                            val lastPickup = document.getTimestamp("lastPickup")
                            if (name != null) {
                                Institution(
                                    id = document.id,
                                    name = name,
                                    lastPickup = lastPickup
                                )
                            } else null
                        }
                        _institutions.value = institutionsList
                    }
                    .addOnFailureListener { e ->
                        Log.e("ExpiringItemsViewModel", "Error loading institutions", e)
                    }
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error loading institutions", e)
            }
        }
    }

    fun increaseQuantity(itemId: String) {
        val currentQuantities = _selectedQuantities.value.toMutableMap()
        val currentQuantity = currentQuantities[itemId] ?: 0
        currentQuantities[itemId] = currentQuantity + 1
        _selectedQuantities.value = currentQuantities
    }

    fun decreaseQuantity(itemId: String) {
        val currentQuantities = _selectedQuantities.value.toMutableMap()
        val currentQuantity = currentQuantities[itemId] ?: 0
        if (currentQuantity > 0) {
            currentQuantities[itemId] = currentQuantity - 1
            _selectedQuantities.value = currentQuantities
        }
    }

    fun getQuantity(itemId: String): Int {
        return _selectedQuantities.value[itemId] ?: 0
    }

    fun clearQuantities() {
        _selectedQuantities.value = emptyMap()
    }

    fun submitExpiringItems() {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val selectedItems = _selectedQuantities.value.filter { it.value > 0 }
                val institutionName = _institutionName.value.trim()

                if (selectedItems.isEmpty()) {
                    _updateState.value = UpdateState.Error("Nenhum item selecionado")
                    return@launch
                }

                if (institutionName.isEmpty()) {
                    _updateState.value = UpdateState.Error("Por favor, preencha o nome da instituição")
                    return@launch
                }

                val institutionData = hashMapOf(
                    "name" to institutionName,
                    "createdAt" to Date(),
                    "lastPickup" to Date(), // Update lastPickup to current time
                    "items" to selectedItems.map { (itemId, quantity) ->
                        val item = _uiState.value.allItems?.find { it.stockItem.id == itemId }
                        hashMapOf(
                            "itemId" to itemId,
                            "productName" to (item?.product?.name ?: "Produto desconhecido"),
                            "quantity" to quantity,
                            "expirationDate" to item?.stockItem?.expirationDate,
                            "daysUntilExpiration" to item?.daysUntilExpiration
                        )
                    }
                )

                firestore.collection("institution")
                    .add(institutionData)
                    .addOnSuccessListener { documentReference ->
                        Log.d("ExpiringItemsViewModel", "DocumentSnapshot written with ID: ${documentReference.id}")

                        selectedItems.forEach { (itemId, quantity) ->
                            updateStockForItem(itemId, quantity)
                        }

                        val currentUser = authRepository.getCurrentUser()
                        viewModelScope.launch {
                            auditRepository.logAction(
                                action = "submit_expiring_items",
                                userId = currentUser?.uid,
                                details = mapOf(
                                    "institutionName" to institutionName,
                                    "totalItems" to selectedItems.size,
                                    "totalQuantity" to selectedItems.values.sum(),
                                    "items" to selectedItems.map { (itemId, quantity) ->
                                        val item = _uiState.value.allItems?.find { it.stockItem.id == itemId }
                                        mapOf(
                                            "itemId" to itemId,
                                            "productName" to (item?.product?.name ?: "Produto desconhecido"),
                                            "quantity" to quantity,
                                            "barcode" to (item?.stockItem?.barcode ?: ""),
                                            "expirationDate" to (item?.stockItem?.expirationDate?.toString()),
                                            "daysUntilExpiration" to (item?.daysUntilExpiration ?: 0)
                                        )
                                    }
                                )
                            )
                        }

                        _updateState.value = UpdateState.Success
                        clearQuantities() // Clear quantities after successful submission
                        _institutionName.value = "" // Clear institution name after successful submission
                    }
                    .addOnFailureListener { e ->
                        Log.e("ExpiringItemsViewModel", "Error adding document", e)
                        _updateState.value = UpdateState.Error("Erro ao atualizar itens: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error submitting expiring items", e)
                _updateState.value = UpdateState.Error("Erro ao atualizar itens: ${e.message}")
            }
        }
    }

    private fun updateStockForItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            val currentStockItem = _uiState.value.allItems?.find { it.stockItem.id == itemId }
            currentStockItem?.let { stockItem ->
                val currentStockQuantity = stockItem.stockItem.quantity ?: 0
                val newQuantity = currentStockQuantity - quantity
                if (newQuantity > 0) {
                    stockItemRepository.updateStockItem(
                        stockItemId = itemId,
                        updates = mapOf("quantity" to newQuantity)
                    )
                    Log.d("ExpiringItemsViewModel", "Updated stock for item $itemId: $currentStockQuantity -> $newQuantity")

                    val currentUser = authRepository.getCurrentUser()
                    auditRepository.logAction(
                        action = "reduce_stock",
                        userId = currentUser?.uid,
                        details = mapOf(
                            "itemId" to itemId,
                            "productName" to (currentStockItem.product?.name ?: "Produto desconhecido"),
                            "barcode" to currentStockItem.stockItem.barcode,
                            "previousQuantity" to currentStockQuantity,
                            "reducedQuantity" to quantity,
                            "newQuantity" to newQuantity
                        )
                    )
                } else if (newQuantity == 0) {
                    val deleteSuccess = stockItemRepository.deleteStockItem(itemId)
                    if (deleteSuccess) {
                        Log.d("ExpiringItemsViewModel", "Deleted item $itemId from stock (quantity reached zero)")

                        val currentUser = authRepository.getCurrentUser()
                        auditRepository.logAction(
                            action = "delete_stock_item",
                            userId = currentUser?.uid,
                            details = mapOf(
                                "itemId" to itemId,
                                "productName" to (currentStockItem.product?.name ?: "Produto desconhecido"),
                                "barcode" to currentStockItem.stockItem.barcode,
                                "previousQuantity" to currentStockQuantity,
                                "reason" to "quantity_reached_zero"
                            )
                        )
                    } else {
                        Log.e("ExpiringItemsViewModel", "Failed to delete item $itemId from stock")
                    }
                } else {
                    Log.w("ExpiringItemsViewModel", "Insufficient stock for item $itemId: $currentStockQuantity < $quantity")
                }
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun loadExpiringItems() {
        viewModelScope.launch {
            try {
                Log.d("ExpiringItemsViewModel", "Starting loadExpiringItems...")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    items = emptyList(),
                    allItems = null
                )

                resetPagination()

                val (expiringItems, hasMore) = stockItemRepository.getExpiringItemsPaginated(
                    daysThreshold = 30,
                    limit = 5,
                    lastExpirationDate = null
                )

                val itemsWithProducts = expiringItems.map { stockItem ->
                    val product = if (stockItem.barcode.isNotEmpty()) {
                        productRepository.getProductByBarcodeId(stockItem.barcode)
                    } else {
                        null
                    }

                    val daysUntilExpiration = stockItem.expirationDate?.let { expDate ->
                        val now = Calendar.getInstance()
                        val expiration = Calendar.getInstance().apply {
                            time = expDate
                        }
                        val diffInMillis = expiration.timeInMillis - now.timeInMillis
                        (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                    } ?: 0

                    ExpiringItemWithProduct(
                        stockItem = stockItem,
                        product = product,
                        daysUntilExpiration = daysUntilExpiration
                    )
                }

                val sortedItems = itemsWithProducts.sortedBy { it.daysUntilExpiration }

                _hasMoreItems.value = hasMore
                _lastLoadedExpirationDate.value = expiringItems.lastOrNull()?.expirationDate

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allItems = sortedItems,
                    items = applyFilterToItemsList(sortedItems, _selectedFilter.value),
                    error = null
                )

                Log.d("ExpiringItemsViewModel", "Successfully loaded ${sortedItems.size} expiring items with pagination")
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error loading expiring items with pagination", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar itens: ${e.message ?: "Erro desconhecido"}"
                )
            }
        }
    }

    fun refresh() {
        loadExpiringItems()
    }

    private fun applyFilterToItems() {
        val currentAllItems = _uiState.value.allItems
        if (currentAllItems != null) {
            val filteredItems = applyFilterToItemsList(currentAllItems, _selectedFilter.value)
            _uiState.value = _uiState.value.copy(items = filteredItems)
        }
    }

    private fun applyFilterToItemsList(
        items: List<ExpiringItemWithProduct>,
        filter: String
    ): List<ExpiringItemWithProduct> {
        return when (filter) {
            "Expirados" -> items.filter { it.daysUntilExpiration < 0 }
            "Por Expirar" -> items.filter { it.daysUntilExpiration >= 0 }
            else -> items // "Todos"
        }
    }
}