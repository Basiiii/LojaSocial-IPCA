package com.lojasocial.app.ui.expiringitems

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.snapshotFlow
import com.lojasocial.app.api.ExpirationCheckResponse
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem
import com.lojasocial.app.domain.stock.ExpiringItemWithProduct
import com.lojasocial.app.domain.stock.ExpiringItemsUiState
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.ui.expiringitems.ExpiringItemsConstants
import com.lojasocial.app.ui.expiringitems.UpdateState
import com.lojasocial.app.ui.expiringitems.components.*
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.LojaSocialSurface
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.TextGray
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Main view for displaying items that are expiring within the threshold period.
 * Provides administrators with a comprehensive view of stock items approaching expiration dates.
 * Handles loading, error, empty, and success states with items sorted by urgency.
 * 
 * @param onNavigateBack callback to navigate back
 * @param expirationRepository optional repository for expiration checks
 * @param viewModel view model for expiring items
 */
@Composable
fun ExpiringItemsView(
    onNavigateBack: () -> Unit,
    expirationRepository: ExpirationRepository? = null,
    viewModel: ExpiringItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedQuantities by viewModel.selectedQuantities.collectAsState()
    val institutionName by viewModel.institutionName.collectAsState()
    val institutions by viewModel.institutions.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val hasMoreItems by viewModel.hasMoreItems.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var showExpirationCheckConfirmation by remember { mutableStateOf(false) }
    var showExpirationCheckLoading by remember { mutableStateOf(false) }
    var showExpirationCheckSuccess by remember { mutableStateOf(false) }
    var showExpirationCheckError by remember { mutableStateOf(false) }
    var expirationCheckError by remember { mutableStateOf<String?>(null) }
    var expirationCheckResult by remember { mutableStateOf<ExpirationCheckResponse?>(null) }
    var showSubmissionSuccessDialog by remember { mutableStateOf(false) }

    // Load more items when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Pair(lastVisibleItemIndex, totalItems)
        }.collect { (lastVisibleIndex, totalItems) ->
            // Load more when user is within 2 items of the end
            if (lastVisibleIndex >= totalItems - 2 && 
                hasMoreItems && 
                !isLoadingMore && 
                !uiState.isLoading) {
                viewModel.loadMoreExpiringItems()
            }
        }
    }
    
    // Auto-load more if filtered list is too short
    LaunchedEffect(uiState, selectedFilter) {
        if (!uiState.isLoading && uiState.error == null) {
            val items = uiState.items
            val allItems = uiState.allItems ?: emptyList()
            
            // Only auto-load if filtered list is short and more items are available
            if (items.size < 5 && 
                hasMoreItems && 
                !isLoadingMore && 
                allItems.size < 20) { // Prevent infinite loading
                viewModel.loadMoreExpiringItems()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
    ) {
        ExpiringItemsTopBar(
            onNavigateBack = onNavigateBack,
            onRefreshClick = if (expirationRepository != null) {
                { showExpirationCheckConfirmation = true }
            } else null
        )

        ExpirationFilters(
            filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
            selectedFilter = selectedFilter,
            onFilterSelected = { filter ->
                viewModel.setSelectedFilter(filter)
            }
        )

        // Institution name field with dropdown
        InstitutionDropdownField(
            institutions = institutions,
            selectedInstitution = institutionName,
            onInstitutionChange = { viewModel.setInstitutionName(it) },
            isLoading = uiState.isLoading
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    ExpiringItemsLoadingState()
                }
                uiState.error != null -> {
                    ExpiringItemsErrorState(
                        errorMessage = uiState.error ?: "Erro desconhecido",
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.items.isEmpty() -> {
                    ExpiringItemsEmptyState()
                }
                else -> {
                    ExpiringItemsList(
                        items = uiState.items,
                        selectedQuantities = selectedQuantities,
                        onQuantityIncrease = { itemId -> viewModel.increaseQuantity(itemId) },
                        onQuantityDecrease = { itemId -> viewModel.decreaseQuantity(itemId) },
                        listState = listState,
                        isLoadingMore = isLoadingMore
                    )
                }
            }

            // Loading overlay for update
            if (updateState is UpdateState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LojaSocialPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A atualizar itens...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        ExpiringItemsBottomBar(
            onSubmitClick = { viewModel.submitExpiringItems() },
            enabled = selectedQuantities.values.any { it > 0 } && 
                     institutionName.trim().isNotEmpty() && 
                     updateState !is UpdateState.Loading
        )
    }
    
    // Handle update state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.Success -> {
                showSubmissionSuccessDialog = true
            }
            is UpdateState.Error -> {
                // Handle error state
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }
    
    // Success Dialog
    if (showSubmissionSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmissionSuccessDialog = false
                viewModel.resetUpdateState()
            },
            containerColor = LojaSocialSurface,
            title = {
                Text("Itens Atualizados!")
            },
            text = {
                Text("Os itens selecionados foram atualizados com sucesso e o stock foi reduzido.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmissionSuccessDialog = false
                        viewModel.resetUpdateState()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Confirmation dialog
    if (showExpirationCheckConfirmation) {
        AlertDialog(
            onDismissRequest = { showExpirationCheckConfirmation = false },
            containerColor = LojaSocialSurface,
            title = {
                Text("Confirmar Verificação")
            },
            text = {
                Text("Tem a certeza que deseja verificar as expirações do stock? Esta ação irá verificar todos os itens e enviar notificações para os itens que estão a expirar em breve.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExpirationCheckConfirmation = false
                        if (expirationRepository != null) {
                            coroutineScope.launch {
                                showExpirationCheckLoading = true
                                showExpirationCheckError = false
                                showExpirationCheckSuccess = false
                                expirationCheckResult = null
                                expirationCheckError = null
                                
                                val result = expirationRepository.checkExpiringItems()
                                
                                showExpirationCheckLoading = false
                                if (result.isSuccess) {
                                    expirationCheckResult = result.getOrNull()
                                    showExpirationCheckSuccess = true
                                    // Refresh the list after successful check
                                    viewModel.refresh()
                                } else {
                                    expirationCheckError = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                                    showExpirationCheckError = true
                                }
                            }
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExpirationCheckConfirmation = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Loading dialog
    if (showExpirationCheckLoading) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = LojaSocialSurface,
            title = {
                Text("A Verificar Expirações")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("A verificar stock e a enviar notificações...")
                }
            },
            confirmButton = {}
        )
    }
    
    // Success dialog
    if (showExpirationCheckSuccess) {
        AlertDialog(
            onDismissRequest = { 
                showExpirationCheckSuccess = false
                expirationCheckResult = null
            },
            containerColor = LojaSocialSurface,
            title = {
                Text("Verificação Concluída")
            },
            text = {
                Column {
                    Text("A verificação de expirações foi concluída com sucesso.")
                    if (expirationCheckResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Itens verificados: ${expirationCheckResult!!.itemCount}")
                        Text("Notificações enviadas: ${expirationCheckResult!!.notificationsSent}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showExpirationCheckSuccess = false
                        expirationCheckResult = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Error dialog
    if (showExpirationCheckError) {
        AlertDialog(
            onDismissRequest = { 
                showExpirationCheckError = false
                expirationCheckError = null
            },
            containerColor = LojaSocialSurface,
            title = {
                Text("Erro na Verificação")
            },
            text = {
                Column {
                    Text("Ocorreu um erro ao verificar as expirações.")
                    if (expirationCheckError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = expirationCheckError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Por favor, tente novamente.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showExpirationCheckError = false
                        expirationCheckError = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * List component displaying all expiring items in a scrollable list.
 * Renders a LazyColumn with expiring items sorted by urgency.
 * 
 * @param modifier modifier for the list
 * @param items list of expiring items
 * @param selectedQuantities map of selected quantities
 * @param onQuantityIncrease callback to increase quantity
 * @param onQuantityDecrease callback to decrease quantity
 * @param listState state of the list
 * @param isLoadingMore whether more items are being loaded
 */
@Composable
private fun ExpiringItemsList(
    modifier: Modifier = Modifier,
    items: List<ExpiringItemWithProduct>,
    selectedQuantities: Map<String, Int>,
    onQuantityIncrease: (String) -> Unit,
    onQuantityDecrease: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    isLoadingMore: Boolean = false
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { item -> item.stockItem.id }
        ) { item ->
            ExpiringItemCard(
                item = item,
                selectedQuantity = selectedQuantities[item.stockItem.id] ?: 0,
                onQuantityIncrease = { onQuantityIncrease(item.stockItem.id) },
                onQuantityDecrease = { onQuantityDecrease(item.stockItem.id) }
            )
        }
        
        // Show loading indicator at the bottom when loading more
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView with sample data.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsViewPreview() {
    LojaSocialTheme {
        // Create mock expiring items for preview
        val mockItems = listOf(
            ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "1",
                    barcode = "123456789",
                    quantity = 5,
                    expirationDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                ),
                product = Product(
                    id = "123456789",
                    name = "Arroz Agulha",
                    brand = "Marca X",
                    category = 1,
                    imageUrl = "https://example.com/arroz.jpg"
                ),
                daysUntilExpiration = 1
            ),
            ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "2",
                    barcode = "987654321",
                    quantity = 10,
                    expirationDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)
                ),
                product = Product(
                    id = "987654321",
                    name = "Azeite Extra Virgem",
                    brand = "Marca Y",
                    category = 1,
                    imageUrl = "https://example.com/azeite.jpg"
                ),
                daysUntilExpiration = 2
            ),
            ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "3",
                    barcode = "555555555",
                    quantity = 3,
                    expirationDate = Date(System.currentTimeMillis())
                ),
                product = Product(
                    id = "555555555",
                    name = "Leite UHT",
                    brand = "Marca Z",
                    category = 1,
                    imageUrl = "https://example.com/leite.jpg"
                ),
                daysUntilExpiration = 0
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            // Institution name field with dropdown
            InstitutionDropdownField(
                institutions = listOf(
                    Institution(
                        id = "1",
                        name = "Cruz Vermelha",
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                    ),
                    Institution(
                        id = "2",
                        name = "Banco Alimentar",
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                    ),
                    Institution(
                        id = "3",
                        name = "Caritas",
                        lastPickup = null
                    )
                ),
                selectedInstitution = "Cruz Vermelha",
                onInstitutionChange = {}
            )
            ExpiringItemsList(
                modifier = Modifier.weight(1f),
                items = mockItems,
                selectedQuantities = mapOf("1" to 2, "2" to 1),
                onQuantityIncrease = {},
                onQuantityDecrease = {}
            )
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = true
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView in loading state.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsViewLoadingPreview() {
    LojaSocialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            // Institution name field with dropdown
            InstitutionDropdownField(
                institutions = emptyList(),
                selectedInstitution = "",
                onInstitutionChange = {}
            )
            ExpiringItemsLoadingState()
            Spacer(modifier = Modifier.weight(1f))
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = false
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView in error state.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsViewErrorPreview() {
    LojaSocialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            // Institution name field with dropdown
            InstitutionDropdownField(
                institutions = emptyList(),
                selectedInstitution = "",
                onInstitutionChange = {}
            )
            ExpiringItemsErrorState(
                errorMessage = "Erro ao carregar itens. Verifique a sua conexão.",
                onRetry = {}
            )
            Spacer(modifier = Modifier.weight(1f))
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = false
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView in empty state.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsViewEmptyPreview() {
    LojaSocialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            // Institution name field with dropdown
            InstitutionDropdownField(
                institutions = emptyList(),
                selectedInstitution = "",
                onInstitutionChange = {}
            )
            ExpiringItemsEmptyState()
            Spacer(modifier = Modifier.weight(1f))
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = false
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView with dropdown opened.
 */
@Preview(showBackground = true, showSystemUi = true, name = "Dropdown Aberto")
@Composable
fun ExpiringItemsViewDropdownOpenedPreview() {
    LojaSocialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            
            // Institution dropdown field with sample data
            InstitutionDropdownField(
                institutions = listOf(
                    Institution(
                        id = "1",
                        name = "Cruz Vermelha",
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 30 * 60 * 1000))
                    ),
                    Institution(
                        id = "2", 
                        name = "Banco Alimentar",
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                    ),
                    Institution(
                        id = "3",
                        name = "Caritas Portugal", 
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000))
                    )
                ),
                selectedInstitution = "Cruz Vermelha",
                onInstitutionChange = {},
                isLoading = false
            )
            
            Spacer(modifier = Modifier.weight(1f))
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = true
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView with selected quantities.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsViewWithSelectionPreview() {
    LojaSocialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBgColor)
        ) {
            ExpiringItemsTopBar(onNavigateBack = {}, onRefreshClick = null)
            ExpirationFilters(
                filters = ExpiringItemsConstants.EXPIRATION_FILTERS,
                selectedFilter = ExpiringItemsConstants.DEFAULT_FILTER,
                onFilterSelected = {}
            )
            // Institution name field with dropdown
            InstitutionDropdownField(
                institutions = listOf(
                    Institution(
                        id = "1",
                        name = "Banco Alimentar",
                        lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000))
                    )
                ),
                selectedInstitution = "Banco Alimentar",
                onInstitutionChange = {}
            )
            ExpiringItemsList(
                modifier = Modifier.weight(1f),
                items = listOf(
                    ExpiringItemWithProduct(
                        stockItem = StockItem(
                            id = "1",
                            barcode = "123456789",
                            quantity = 5,
                            expirationDate = Date(System.currentTimeMillis())
                        ),
                        product = Product(
                            id = "123456789",
                            name = "Leite UHT",
                            brand = "Marca Z",
                            category = 1,
                            imageUrl = "https://example.com/leite.jpg"
                        ),
                        daysUntilExpiration = 0
                    )
                ),
                selectedQuantities = mapOf("1" to 3),
                onQuantityIncrease = {},
                onQuantityDecrease = {}
            )
            ExpiringItemsBottomBar(
                onSubmitClick = {},
                enabled = true
            )
        }
    }
}