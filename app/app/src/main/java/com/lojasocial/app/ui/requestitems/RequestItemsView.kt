package com.lojasocial.app.ui.requestitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.ui.requestitems.components.CategoryFilters
import com.lojasocial.app.ui.requestitems.components.ProductItemRow
import com.lojasocial.app.ui.requestitems.components.RequestItemsBottomBar
import com.lojasocial.app.ui.requestitems.components.RequestItemsTopAppBar
import com.lojasocial.app.ui.requestitems.components.RequestStatusHeader
import com.lojasocial.app.ui.requestitems.components.SearchBar
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun RequestItemsView(
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {},
    viewModel: RequestItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val productQuantities by viewModel.productQuantities.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Todos", "Alimentar", "Limpeza", "Higiene")
    var selectedCategory by remember { mutableStateOf("Todos") }

    RequestItemsContent(
        uiState = uiState,
        productQuantities = productQuantities,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        categories = categories,
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it },
        onAddProduct = { viewModel.onAddProduct(it) },
        onRemoveProduct = { viewModel.onRemoveProduct(it) },
        onClearQuantities = { viewModel.clearQuantities() },
        onBackClick = onBackClick,
        onSubmitClick = onSubmitClick
    )
}

@Composable
fun RequestItemsContent(
    uiState: RequestItemsUiState,
    productQuantities: Map<Int, Int>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddProduct: (Int) -> Unit,
    onRemoveProduct: (Int) -> Unit,
    onClearQuantities: () -> Unit,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val totalItemsSelected = productQuantities.values.sum()
    val maxItems = 10
    val progress = (totalItemsSelected.toFloat() / maxItems.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RequestItemsTopAppBar(onBackClick = onBackClick) },
        bottomBar = {
            RequestItemsBottomBar(
                onSubmitClick = onSubmitClick,
                enabled = totalItemsSelected > 0
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            RequestStatusHeader(
                totalItemsSelected = totalItemsSelected,
                maxItems = maxItems,
                progress = progress,
                onClearClick = onClearQuantities
            )

            SearchBar(searchQuery = searchQuery, onSearchQueryChange = onSearchQueryChange)

            CategoryFilters(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )

            when (uiState) {
                is RequestItemsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LojaSocialPrimary)
                    }
                }
                is RequestItemsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        val filteredProducts = uiState.products.filter { product ->
                            (selectedCategory == "Todos" || product.category == selectedCategory) &&
                                    (searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true))
                        }

                        items(filteredProducts) { product ->
                            ProductItemRow(
                                product = product,
                                quantity = productQuantities[product.id] ?: 0,
                                onAdd = { onAddProduct(product.id) },
                                onRemove = { onRemoveProduct(product.id) },
                                enabled = totalItemsSelected < maxItems
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 1.dp,
                                color = BorderColor
                            )
                        }
                    }
                }
                is RequestItemsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Erro ao carregar produtos", color = TextGray)
                    }
                }
            }
        }
    }
}

val previewCategories = listOf("Todos", "Alimentar", "Limpeza", "Higiene")

@Preview(showBackground = true, name = "1. Sucesso (Com Dados)")
@Composable
fun RequestItemsViewPreview() {
    val mockProducts = listOf(
        com.lojasocial.app.domain.RequestItem(1, "Arroz Agulha", "Alimentar", 50),
        com.lojasocial.app.domain.RequestItem(2, "Massa Esparguete", "Alimentar", 30),
        com.lojasocial.app.domain.RequestItem(3, "Leite UHT", "Alimentar", 0),
        com.lojasocial.app.domain.RequestItem(4, "Lixívia", "Limpeza", 10),
        com.lojasocial.app.domain.RequestItem(5, "Sabonete", "Higiene", 25)
    )

    RequestItemsContent(
        uiState = RequestItemsUiState.Success(mockProducts),
        productQuantities = mapOf(1 to 2, 2 to 1),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = "Todos",
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}

@Preview(showBackground = true, name = "2. Estado Loading")
@Composable
fun PreviewLoading() {
    RequestItemsContent(
        uiState = RequestItemsUiState.Loading,
        productQuantities = emptyMap(),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = "Todos",
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}

@Preview(showBackground = true, name = "3. Estado Erro")
@Composable
fun PreviewError() {
    RequestItemsContent(
        uiState = RequestItemsUiState.Error,
        productQuantities = emptyMap(),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = "Todos",
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}

@Preview(showBackground = true, name = "4. Lista Vazia")
@Composable
fun PreviewEmptyList() {
    RequestItemsContent(
        uiState = RequestItemsUiState.Success(emptyList()),
        productQuantities = emptyMap(),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = "Todos",
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}

@Preview(showBackground = true, name = "5. Limite Atingido (10/10)")
@Composable
fun PreviewLimitReached() {
    val mockProducts = listOf(
        com.lojasocial.app.domain.RequestItem(1, "Arroz", "Alimentar", 50),
        com.lojasocial.app.domain.RequestItem(2, "Massa", "Alimentar", 50)
    )

    val quantities = mapOf(1 to 5, 2 to 5)

    RequestItemsContent(
        uiState = RequestItemsUiState.Success(mockProducts),
        productQuantities = quantities,
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = "Todos",
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}