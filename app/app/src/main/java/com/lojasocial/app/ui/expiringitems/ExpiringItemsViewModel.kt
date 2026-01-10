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
 * 
 * This ViewModel is responsible for:
 * - Loading stock items that are expiring within the threshold period (3 days)
 * - Fetching product information for each expiring item
 * - Calculating days until expiration for each item
 * - Sorting items by urgency (soonest expiration first)
 * - Managing loading, error, and success states
 * - Updating stock quantities and deleting items when quantity reaches zero
 * - Logging all actions to audit logs
 * 
 * The ViewModel uses StateFlow to expose UI state, making it easy for the View
 * to observe and react to changes reactively.
 * 
 * @param stockItemRepository Repository for accessing stock item data
 * @param productRepository Repository for accessing product information
 * @param auditRepository Repository for logging audit actions
 * @param authRepository Repository for authentication data
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

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadExpiringItems()
        loadInstitutions()
    }

    /**
     * Updates the selected filter and refreshes the filtered items.
     */
    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
        applyFilterToItems()
    }

    /**
     * Updates institution name.
     */
    fun setInstitutionName(name: String) {
        _institutionName.value = name
    }

    /**
     * Loads institutions from Firestore, sorted by lastPickup date.
     */
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

    /**
     * Increases the quantity for a specific item.
     */
    fun increaseQuantity(itemId: String) {
        val currentQuantities = _selectedQuantities.value.toMutableMap()
        val currentQuantity = currentQuantities[itemId] ?: 0
        currentQuantities[itemId] = currentQuantity + 1
        _selectedQuantities.value = currentQuantities
    }

    /**
     * Decreases the quantity for a specific item.
     */
    fun decreaseQuantity(itemId: String) {
        val currentQuantities = _selectedQuantities.value.toMutableMap()
        val currentQuantity = currentQuantities[itemId] ?: 0
        if (currentQuantity > 0) {
            currentQuantities[itemId] = currentQuantity - 1
            _selectedQuantities.value = currentQuantities
        }
    }

    /**
     * Gets the selected quantity for a specific item.
     */
    fun getQuantity(itemId: String): Int {
        return _selectedQuantities.value[itemId] ?: 0
    }

    /**
     * Clears all selected quantities.
     */
    fun clearQuantities() {
        _selectedQuantities.value = emptyMap()
    }

    /**
     * Submits the selected expiring items to Firestore.
     * 
     * Creates a document in the 'institution' collection with the selected items
     * and metadata including the provided institution name and createdAt timestamp.
     */
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

                // Create the document data
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

                // Save to Firestore
                firestore.collection("institution")
                    .add(institutionData)
                    .addOnSuccessListener { documentReference ->
                        Log.d("ExpiringItemsViewModel", "DocumentSnapshot written with ID: ${documentReference.id}")
                        
                        // Update stock quantities for selected items
                        selectedItems.forEach { (itemId, quantity) ->
                            updateStockForItem(itemId, quantity)
                        }
                        
                        // Log the submission action to audit logs
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

    /**
     * Updates stock quantity for a specific item after submission.
     * Deletes item if quantity reaches zero, otherwise updates quantity.
     * @param itemId The ID of the stock item to update
     * @param quantity The quantity to subtract from stock
     */
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
                    
                    // Log stock reduction action to audit logs
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
                    // Delete item from stock when quantity reaches zero
                    val deleteSuccess = stockItemRepository.deleteStockItem(itemId)
                    if (deleteSuccess) {
                        Log.d("ExpiringItemsViewModel", "Deleted item $itemId from stock (quantity reached zero)")
                        
                        // Log item deletion action to audit logs
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

    /**
     * Resets the update state to Idle.
     */
    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Loads expiring items from the repository and enriches them with product information.
     * 
     * This method:
     * 1. Queries stock items expiring within 30 days (to get both expired and soon-to-expire)
     * 2. Fetches product details for each item using the barcode
     * 3. Calculates days until expiration
     * 4. Sorts items by urgency (soonest first)
     * 5. Updates the UI state accordingly
     * 
     * Errors are caught and displayed to the user via the error state.
     */
    fun loadExpiringItems() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Get expiring items (within 30 days to include both expired and soon-to-expire)
                val expiringItems = stockItemRepository.getExpiringItems(30)
                Log.d("ExpiringItemsViewModel", "Found ${expiringItems.size} expiring items")
                
                // Fetch product details for each item
                val itemsWithProducts = expiringItems.map { stockItem ->
                    val product = if (stockItem.barcode.isNotEmpty()) {
                        productRepository.getProductByBarcodeId(stockItem.barcode)
                    } else {
                        null
                    }
                    
                    // Calculate days until expiration (negative for already expired)
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
                
                // Sort by expiration date (soonest first)
                val sortedItems = itemsWithProducts.sortedBy { it.daysUntilExpiration }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allItems = sortedItems,
                    items = applyFilterToItemsList(sortedItems, _selectedFilter.value),
                    error = null
                )
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error loading expiring items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar itens: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes the expiring items list.
     * 
     * This method reloads all expiring items from the repository, useful for
     * manual refresh actions or after retrying from an error state.
     */
    fun refresh() {
        loadExpiringItems()
    }

    /**
     * Applies the current filter to the loaded items.
     */
    private fun applyFilterToItems() {
        val currentAllItems = _uiState.value.allItems
        if (currentAllItems != null) {
            val filteredItems = applyFilterToItemsList(currentAllItems, _selectedFilter.value)
            _uiState.value = _uiState.value.copy(items = filteredItems)
        }
    }

    /**
     * Filters items based on the selected filter.
     */
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
