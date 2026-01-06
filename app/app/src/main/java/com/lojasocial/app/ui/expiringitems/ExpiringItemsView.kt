package com.lojasocial.app.ui.expiringitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.api.ExpirationCheckResponse
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.data.model.StockItem
import com.lojasocial.app.domain.ExpiringItemWithProduct
import com.lojasocial.app.domain.ExpiringItemsUiState
import com.lojasocial.app.repository.ExpirationRepository
import com.lojasocial.app.ui.expiringitems.components.*
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialTheme
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Main view for displaying items that are expiring within the threshold period.
 * 
 * This screen provides administrators with a comprehensive view of stock items
 * that are approaching their expiration dates (within 3 days). It displays:
 * - Product information (name, brand, image)
 * - Stock quantity
 * - Expiration date
 * - Days until expiration with color-coded urgency indicators
 * 
 * The view handles multiple states:
 * - **Loading**: Shows a progress indicator while fetching data
 * - **Error**: Displays error message with retry option
 * - **Empty**: Shows message when no items are expiring
 * - **Success**: Displays list of expiring items sorted by urgency
 * 
 * Items are automatically sorted by expiration date (soonest first) to help
 * administrators prioritize which items need immediate attention.
 * 
 * @param onNavigateBack Callback invoked when the back button is clicked, navigates to Employee Portal
 * @param expirationRepository Optional repository for triggering expiration checks
 * @param viewModel The view model managing the expiring items state and business logic
 * 
 * @see ExpiringItemsViewModel The ViewModel managing the state
 * @see ExpiringItemCard Component for displaying individual expiring items
 */
@Composable
fun ExpiringItemsView(
    onNavigateBack: () -> Unit,
    expirationRepository: ExpirationRepository? = null,
    viewModel: ExpiringItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showExpirationCheckConfirmation by remember { mutableStateOf(false) }
    var showExpirationCheckLoading by remember { mutableStateOf(false) }
    var showExpirationCheckSuccess by remember { mutableStateOf(false) }
    var showExpirationCheckError by remember { mutableStateOf(false) }
    var expirationCheckError by remember { mutableStateOf<String?>(null) }
    var expirationCheckResult by remember { mutableStateOf<ExpirationCheckResponse?>(null) }

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
                ExpiringItemsList(items = uiState.items)
            }
        }
    }
    
    // Confirmation dialog
    if (showExpirationCheckConfirmation) {
        AlertDialog(
            onDismissRequest = { showExpirationCheckConfirmation = false },
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
 * 
 * Renders a LazyColumn with all expiring items, each displayed as an ExpiringItemCard.
 * Items are automatically sorted by urgency (days until expiration) with the most
 * urgent items appearing first.
 * 
 * @param items List of expiring items to display, sorted by urgency
 */
@Composable
private fun ExpiringItemsList(
    items: List<ExpiringItemWithProduct>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ExpiringItemCard(item = item)
        }
    }
}

/**
 * Preview composable for the ExpiringItemsView with sample data.
 * 
 * Shows the view in a success state with multiple expiring items to demonstrate
 * the UI layout and styling.
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
                    expirationDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // Tomorrow
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
                    expirationDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000) // 2 days
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
                    expirationDate = Date(System.currentTimeMillis()) // Today
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
            ExpiringItemsList(items = mockItems)
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
            ExpiringItemsLoadingState()
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
            ExpiringItemsErrorState(
                errorMessage = "Erro ao carregar itens. Verifique a sua conexão.",
                onRetry = {}
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
            ExpiringItemsEmptyState()
        }
    }
}
