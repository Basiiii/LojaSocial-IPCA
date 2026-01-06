package com.lojasocial.app.ui.requestitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.domain.RequestItem
import com.lojasocial.app.ui.requestitems.components.CategoryFilters
import com.lojasocial.app.ui.requestitems.components.ProductItemRow
import com.lojasocial.app.ui.requestitems.components.RequestItemsBottomBar
import com.lojasocial.app.ui.requestitems.components.RequestItemsTopAppBar
import com.lojasocial.app.ui.requestitems.components.RequestStatusHeader
import com.lojasocial.app.ui.requestitems.components.SearchBar
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray
import com.lojasocial.app.ui.requestitems.RequestItemsConstants

@Composable
fun RequestItemsView(
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {},
    viewModel: RequestItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val productQuantities by viewModel.productQuantities.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()

    val isSubmitting = submissionState is SubmissionState.Loading

    LaunchedEffect(submissionState) {
        val currentSubmissionState = submissionState
        when (currentSubmissionState) {
            is SubmissionState.Success -> {
                viewModel.resetSubmissionState()
                onSubmitClick()
            }

            is SubmissionState.Error -> {
                viewModel.resetSubmissionState()
                onSubmitClick()
            }

            else -> {}
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val categories = RequestItemsConstants.PRODUCT_CATEGORIES
    var selectedCategory by remember { mutableStateOf(RequestItemsConstants.DEFAULT_CATEGORY) }

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
        isSubmitting = isSubmitting,
        onSubmitClick = {
            if (!isSubmitting) {
                viewModel.submitRequest()
            }
        },
        onLoadMore = { viewModel.fetchProducts(isLoadMore = true) }
    )
}

@Composable
fun RequestItemsContent(
    uiState: RequestItemsUiState,
    productQuantities: Map<String, Int>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddProduct: (String) -> Unit,
    onRemoveProduct: (String) -> Unit,
    onClearQuantities: () -> Unit,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    isSubmitting: Boolean = false,
    onLoadMore: () -> Unit
) {
    val totalItemsSelected = productQuantities.values.sum()
    val maxItems = RequestItemsConstants.MAX_ITEMS
    val progress = (totalItemsSelected.toFloat() / maxItems.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RequestItemsTopAppBar(onBackClick = onBackClick) },
        bottomBar = {
            RequestItemsBottomBar(
                onSubmitClick = onSubmitClick,
                enabled = totalItemsSelected > 0 && !isSubmitting
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LojaSocialPrimary)
                        }
                    }

                    is RequestItemsUiState.Success -> {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            val filteredProducts = uiState.products.filter { product ->
                                (selectedCategory == RequestItemsConstants.DEFAULT_CATEGORY || product.product.category == RequestItemsConstants.PRODUCT_CATEGORIES.indexOf(selectedCategory)) &&
                                        (searchQuery.isBlank() || product.product.name.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ))
                            }

                            items(filteredProducts) { requestItem ->
                                ProductItemRow(
                                    requestItem = requestItem,
                                    quantity = productQuantities[requestItem.id] ?: 0,
                                    onAdd = {
                                        if (!isSubmitting) onAddProduct(requestItem.id)
                                    },
                                    onRemove = {
                                        if (!isSubmitting) onRemoveProduct(requestItem.id)
                                    },
                                    enabled = totalItemsSelected < maxItems && !isSubmitting
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color = BorderColor
                                )
                            }
                        }

                        val isScrolledToEnd by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                                if (layoutInfo.totalItemsCount == 0) {
                                    false
                                } else {
                                    val lastVisibleItem = visibleItemsInfo.last()
                                    lastVisibleItem.index + 1 == layoutInfo.totalItemsCount
                                }
                            }
                        }

                        LaunchedEffect(isScrolledToEnd) {
                            if (isScrolledToEnd) {
                                onLoadMore()
                            }
                        }
                    }

                    is RequestItemsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Erro ao carregar produtos", color = TextGray)
                        }
                    }
                }
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LojaSocialPrimary)
                    }
                }
            }
        }
    }
}

val previewCategories = RequestItemsConstants.PRODUCT_CATEGORIES

@Preview(showBackground = true)
@Composable
fun RequestItemsViewPreview() {
    val mockProducts = listOf(
        RequestItem(
            id = "1",
            product = Product(
                name = "Arroz Agulha",
                category = 1,
            )
        ),
    )

    RequestItemsContent(
        uiState = RequestItemsUiState.Success(mockProducts),
        productQuantities = mapOf("1" to 2, "2" to 1),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = RequestItemsConstants.DEFAULT_CATEGORY,
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {},
        isSubmitting = false,
        onLoadMore = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLoading() {
    RequestItemsContent(
        uiState = RequestItemsUiState.Loading,
        productQuantities = emptyMap(),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = RequestItemsConstants.DEFAULT_CATEGORY,
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {},
        isSubmitting = false,
        onLoadMore = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSubmitting() {
    val mockProducts = listOf(
        RequestItem(
            id = "1",
            product = Product(
                name = "Arroz Agulha",
                category = 1,
            )
        ),
    )
    RequestItemsContent(
        uiState = RequestItemsUiState.Success(mockProducts),
        productQuantities = mapOf("1" to 2),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = RequestItemsConstants.DEFAULT_CATEGORY,
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {},
        isSubmitting = true,
        onLoadMore = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewError() {
    RequestItemsContent(
        uiState = RequestItemsUiState.Error,
        productQuantities = emptyMap(),
        searchQuery = "",
        onSearchQueryChange = {},
        categories = previewCategories,
        selectedCategory = RequestItemsConstants.DEFAULT_CATEGORY,
        onCategorySelected = {},
        onAddProduct = {},
        onRemoveProduct = {},
        onClearQuantities = {},
        onBackClick = {},
        onSubmitClick = {},
        onLoadMore = {}
    )
}
