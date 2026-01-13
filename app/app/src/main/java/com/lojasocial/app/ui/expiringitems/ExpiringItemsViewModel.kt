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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.lojasocial.app.ui.expiringitems.components.Institution
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Enum representing the state of the expiring items update operation.
 */
sealed class UpdateState {
    /**
     * Initial state, no operation in progress.
     */
    object Idle : UpdateState()
    /**
     * Operation is in progress.
     */
    object Loading : UpdateState()
    /**
     * Operation completed successfully.
     */
    object Success : UpdateState()
    /**
     * Operation failed with an error message.
     */
    data class Error(val message: String) : UpdateState()
}

/**
 * ViewModel for managing expiring items state and business logic.
 * * This ViewModel is responsible for:
 * - Loading stock items that are expiring within the threshold period (30 days, including already expired)
 * - Fetching product information for each expiring item
 * - Calculating days until expiration for each item
 * - Sorting items by urgency (soonest expiration first)
 * - Managing loading, error, and success states
 * * The ViewModel uses StateFlow to expose UI state, making it easy for the View
 * to observe and react to changes reactively.
 * * @param stockItemRepository Repository for accessing stock item data
 * @param productRepository Repository for accessing product information
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

    private val _lastLoadedExpirationDate = MutableStateFlow<Date?>(null)

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

                val documentReference = firestore.collection("institution")
                    .add(institutionData)
                    .await()
                
                Log.d("ExpiringItemsViewModel", "DocumentSnapshot written with ID: ${documentReference.id}")

                val updateResults = selectedItems.map { (itemId, quantity) ->
                    async { updateStockForItem(itemId, quantity) }
                }.awaitAll()

                val allUpdatesSuccessful = updateResults.all { it }
                if (!allUpdatesSuccessful) {
                    Log.w("ExpiringItemsViewModel", "Some stock updates failed")
                }

                updateLocalItemsAfterSubmission(selectedItems)

                val currentUser = authRepository.getCurrentUser()
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

                _updateState.value = UpdateState.Success
                clearQuantities() // Clear quantities after successful submission
                _institutionName.value = "" // Clear institution name after successful submission

            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error submitting expiring items", e)
                _updateState.value = UpdateState.Error("Erro ao atualizar itens: ${e.message}")
            }
        }
    }

    private fun updateLocalItemsAfterSubmission(selectedItems: Map<String, Int>) {
        val currentAllItems = _uiState.value.allItems?.toMutableList() ?: return
        
        // Update or remove items based on the submission
        val updatedItems = currentAllItems.mapNotNull { item ->
            val itemId = item.stockItem.id
            val selectedQuantity = selectedItems[itemId] ?: 0
            
            if (selectedQuantity > 0) {
                val currentQuantity = item.stockItem.quantity
                val newQuantity = currentQuantity - selectedQuantity
                
                if (newQuantity > 0) {
                    // Update the item with new quantity
                    item.copy(
                        stockItem = item.stockItem.copy(quantity = newQuantity)
                    )
                } else {
                    // Remove the item if quantity reached zero
                    null
                }
            } else {
                // Keep items that were not selected
                item
            }
        }
        
        // Update the UI state with the modified list
        val sortedUpdatedItems = updatedItems.sortedBy { it.daysUntilExpiration }
        _uiState.value = _uiState.value.copy(
            allItems = sortedUpdatedItems,
            items = applyFilterToItemsList(sortedUpdatedItems, _selectedFilter.value)
        )
        
        Log.d("ExpiringItemsViewModel", "Updated local items: removed ${currentAllItems.size - updatedItems.size} items, updated quantities for selected items")
    }

    private suspend fun updateStockForItem(itemId: String, quantity: Int): Boolean {
        return try {
            val currentStockItem = _uiState.value.allItems?.find { it.stockItem.id == itemId }
            currentStockItem?.let { stockItem ->
                val currentStockQuantity = stockItem.stockItem.quantity
                val newQuantity = currentStockQuantity - quantity
                if (newQuantity > 0) {
                    val success = stockItemRepository.updateStockItem(
                        stockItemId = itemId,
                        updates = mapOf("quantity" to newQuantity)
                    )
                    if (success) {
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
                        true
                    } else {
                        Log.e("ExpiringItemsViewModel", "Failed to update stock for item $itemId")
                        false
                    }
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
                        true
                    } else {
                        Log.e("ExpiringItemsViewModel", "Failed to delete item $itemId from stock")
                        false
                    }
                } else {
                    Log.w("ExpiringItemsViewModel", "Insufficient stock for item $itemId: $currentStockQuantity < $quantity")
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("ExpiringItemsViewModel", "Error updating stock for item $itemId", e)
            false
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
                    
                    // Calculate days until expiration (negative means expired)
                    val daysUntilExpiration = stockItem.expirationDate?.let { expDate ->
                        val now = Calendar.getInstance()
                        val expiration = Calendar.getInstance().apply {
                            time = expDate
                        }
                        val diffInMillis = expiration.timeInMillis - now.timeInMillis
                        (diffInMillis / (1000 * 60 * 60 * 24)).toInt() // Allow negative for expired items
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