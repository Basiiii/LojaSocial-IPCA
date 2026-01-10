package com.lojasocial.app.ui.stock

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.product.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ProductWithStock(
    val product: Product,
    val totalStock: Int,
    val categoryName: String,
    val earliestExpiryDate: Date? = null, // Earliest expiry date among all stock items for this product
    val hasExpiryDate: Boolean = false // Whether any stock item has an expiry date
)

enum class SortOption {
    NAME, // Sort by product name
    EXPIRY_DATE // Sort by earliest expiry date
}

data class StockListUiState(
    val isLoading: Boolean = true,
    val products: List<ProductWithStock> = emptyList(),
    val filteredProducts: List<ProductWithStock> = emptyList(),
    val selectedCategories: Set<ProductCategory> = emptySet(),
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.NAME,
    val hideNonPerishable: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StockListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val stockItemRepository: StockItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockListUiState(isLoading = true))
    val uiState: StateFlow<StockListUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Get all products
                val products = productRepository.getAllProducts()
                Log.d("StockListViewModel", "Found ${products.size} products")
                
                // Get all stock items
                val stockItems = stockItemRepository.getAllStockItems()
                Log.d("StockListViewModel", "Found ${stockItems.size} stock items")
                
                // Group stock items by barcode and calculate total stock and expiry info (optimized single pass)
                val stockByBarcode = stockItems.groupBy { it.barcode }
                    .mapValues { (_, items) -> 
                        var totalStock = 0
                        var earliestExpiry: Date? = null
                        var hasExpiry = false
                        
                        items.forEach { item ->
                            totalStock += item.quantity
                            item.expirationDate?.let { expDate ->
                                hasExpiry = true
                                if (earliestExpiry == null || expDate.before(earliestExpiry)) {
                                    earliestExpiry = expDate
                                }
                            }
                        }
                        
                        Triple(totalStock, earliestExpiry, hasExpiry)
                    }
                
                // Create ProductWithStock list (unsorted for now)
                val productsWithStock = products.map { product ->
                    val (totalStock, earliestExpiry, hasExpiry) = stockByBarcode[product.id] 
                        ?: Triple(0, null as Date?, false)
                    val categoryName = when (ProductCategory.fromId(product.category)) {
                        ProductCategory.ALIMENTAR -> "Alimentar"
                        ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                        ProductCategory.CASA -> "Limpeza"
                        null -> "Geral"
                    }
                    
                    ProductWithStock(
                        product = product,
                        totalStock = totalStock,
                        categoryName = categoryName,
                        earliestExpiryDate = earliestExpiry,
                        hasExpiryDate = hasExpiry
                    )
                }.filter { it.totalStock > 0 } // Only show products with stock
                
                // Apply default sorting (by name)
                val sorted = productsWithStock.sortedBy { it.product.name }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = productsWithStock,
                    filteredProducts = sorted,
                    sortBy = SortOption.NAME,
                    hideNonPerishable = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("StockListViewModel", "Error loading products", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar produtos: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadProducts()
    }

    fun toggleCategory(category: ProductCategory) {
        val currentSelected = _uiState.value.selectedCategories.toMutableSet()
        if (currentSelected.contains(category)) {
            currentSelected.remove(category)
        } else {
            currentSelected.add(category)
        }
        applyFilters(
            currentSelected, 
            _uiState.value.searchQuery,
            _uiState.value.sortBy,
            _uiState.value.hideNonPerishable
        )
    }

    fun clearFilters() {
        applyFilters(
            emptySet(), 
            "",
            SortOption.NAME,
            false
        )
    }

    fun setSearchQuery(query: String) {
        applyFilters(
            _uiState.value.selectedCategories, 
            query,
            _uiState.value.sortBy,
            _uiState.value.hideNonPerishable
        )
    }

    fun setSortBy(sortOption: SortOption) {
        applyFilters(
            _uiState.value.selectedCategories,
            _uiState.value.searchQuery,
            sortOption,
            _uiState.value.hideNonPerishable
        )
    }

    fun setHideNonPerishable(hide: Boolean) {
        applyFilters(
            _uiState.value.selectedCategories,
            _uiState.value.searchQuery,
            _uiState.value.sortBy,
            hide
        )
    }

    private fun applyFilters(
        selectedCategories: Set<ProductCategory>, 
        searchQuery: String,
        sortBy: SortOption,
        hideNonPerishable: Boolean
    ) {
        val allProducts = _uiState.value.products
        var filtered = allProducts
        
        // Filter by category
        if (selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { productWithStock ->
                val category = ProductCategory.fromId(productWithStock.product.category)
                category != null && selectedCategories.contains(category)
            }
        }
        
        // Filter by search query (name or brand)
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { productWithStock ->
                productWithStock.product.name.contains(searchQuery, ignoreCase = true) ||
                productWithStock.product.brand.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Filter out non-perishable items if enabled
        if (hideNonPerishable) {
            filtered = filtered.filter { it.hasExpiryDate }
        }
        
        // Sort the filtered results
        val sorted = when (sortBy) {
            SortOption.NAME -> filtered.sortedBy { it.product.name }
            SortOption.EXPIRY_DATE -> filtered.sortedWith(compareBy(
                // First, items with expiry dates (0) come before those without (1)
                { if (it.earliestExpiryDate == null) 1 else 0 },
                // Then sort by expiry date (earliest first)
                { it.earliestExpiryDate ?: Date(Long.MAX_VALUE) },
                // Finally sort by name for items without expiry dates
                { it.product.name }
            ))
        }
        
        _uiState.value = _uiState.value.copy(
            selectedCategories = selectedCategories,
            searchQuery = searchQuery,
            sortBy = sortBy,
            hideNonPerishable = hideNonPerishable,
            filteredProducts = sorted
        )
    }
}
