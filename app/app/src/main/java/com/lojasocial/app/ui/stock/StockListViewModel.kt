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
import javax.inject.Inject

data class ProductWithStock(
    val product: Product,
    val totalStock: Int,
    val categoryName: String
)

data class StockListUiState(
    val isLoading: Boolean = true,
    val products: List<ProductWithStock> = emptyList(),
    val filteredProducts: List<ProductWithStock> = emptyList(),
    val selectedCategories: Set<ProductCategory> = emptySet(),
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
                
                // Group stock items by barcode and calculate total stock
                val stockByBarcode = stockItems.groupBy { it.barcode }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } }
                
                // Create ProductWithStock list
                val productsWithStock = products.map { product ->
                    val totalStock = stockByBarcode[product.id] ?: 0
                    val categoryName = when (ProductCategory.fromId(product.category)) {
                        ProductCategory.ALIMENTAR -> "Alimentar"
                        ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                        ProductCategory.CASA -> "Limpeza"
                        null -> "Geral"
                    }
                    
                    ProductWithStock(
                        product = product,
                        totalStock = totalStock,
                        categoryName = categoryName
                    )
                }.filter { it.totalStock > 0 } // Only show products with stock
                .sortedBy { it.product.name }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = productsWithStock,
                    filteredProducts = productsWithStock,
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
        applyFilters(currentSelected)
    }

    fun clearFilters() {
        applyFilters(emptySet())
    }

    private fun applyFilters(selectedCategories: Set<ProductCategory>) {
        val allProducts = _uiState.value.products
        val filtered = if (selectedCategories.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { productWithStock ->
                val category = ProductCategory.fromId(productWithStock.product.category)
                category != null && selectedCategories.contains(category)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            selectedCategories = selectedCategories,
            filteredProducts = filtered
        )
    }
}
