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
import com.lojasocial.app.repository.ProductsRepository
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
    viewModel: RequestItemsViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Todos", "Alimentar", "Limpeza", "Higiene")
    var selectedCategory by remember { mutableStateOf("Todos") }

    val uiState by viewModel.uiState.collectAsState()
    val productQuantities by viewModel.productQuantities.collectAsState()

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
                onClearClick = { viewModel.clearQuantities() }
            )

            SearchBar(searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it })

            CategoryFilters(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            when (val state = uiState) {
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
                        items(state.products.filter {
                            (selectedCategory == "Todos" || it.category == selectedCategory) &&
                                    (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
                        }) { product ->
                            ProductItemRow(
                                product = product,
                                quantity = productQuantities[product.id] ?: 0,
                                onAdd = { viewModel.onAddProduct(product.id) },
                                onRemove = { viewModel.onRemoveProduct(product.id) },
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

@Preview(showBackground = true)
@Composable
fun RequestItemsViewPreview() {
    val viewModel = RequestItemsViewModel(ProductsRepository())
    RequestItemsView(viewModel = viewModel)
}
