package com.lojasocial.app.ui.expiringitems

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedQuantities by viewModel.selectedQuantities.collectAsState()
    val institutionName by viewModel.institutionName.collectAsState()
    val institutions by viewModel.institutions.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showExpirationCheckConfirmation by remember { mutableStateOf(false) }
    var showExpirationCheckLoading by remember { mutableStateOf(false) }
    var showExpirationCheckSuccess by remember { mutableStateOf(false) }
    var showExpirationCheckError by remember { mutableStateOf(false) }
    var expirationCheckError by remember { mutableStateOf<String?>(null) }
    var expirationCheckResult by remember { mutableStateOf<ExpirationCheckResponse?>(null) }
    var showSubmissionSuccessDialog by remember { mutableStateOf(false) }

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
                        onQuantityDecrease = { itemId -> viewModel.decreaseQuantity(itemId) }
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
                // Handle error state (could show snackbar or dialog)
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
 * 
 * Renders a LazyColumn with all expiring items, each displayed as an ExpiringItemCard.
 * Items are automatically sorted by urgency (days until expiration) with the most
 * urgent items appearing first.
 * 
 * @param modifier Modifier for the list component
 * @param items List of expiring items to display, sorted by urgency
 * @param selectedQuantities Map of selected quantities for each item
 * @param onQuantityIncrease Callback when quantity is increased for an item
 * @param onQuantityDecrease Callback when quantity is decreased for an item
 */
@Composable
private fun ExpiringItemsList(
    modifier: Modifier = Modifier,
    items: List<ExpiringItemWithProduct>,
    selectedQuantities: Map<String, Int>,
    onQuantityIncrease: (String) -> Unit,
    onQuantityDecrease: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ExpiringItemCard(
                item = item,
                selectedQuantity = selectedQuantities[item.stockItem.id] ?: 0,
                onQuantityIncrease = { onQuantityIncrease(item.stockItem.id) },
                onQuantityDecrease = { onQuantityDecrease(item.stockItem.id) }
            )
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
            
            // Simulate opened dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    // Text field (simulating the dropdown field)
                    OutlinedTextField(
                        value = "Cruz",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = "Nome da Instituição", 
                                color = TextGray,
                                fontSize = 16.sp
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Business, 
                                contentDescription = null, 
                                tint = TextGray,
                                modifier = Modifier.size(20.dp)
                            ) 
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Expandir",
                                tint = TextGray,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = BorderColor,
                            focusedBorderColor = LojaSocialPrimary,
                            cursorColor = LojaSocialPrimary
                        ),
                        singleLine = true
                    )
                    
                    // Opened dropdown menu
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Recent institutions header
                            Text(
                                text = "Instituições Recentes:",
                                modifier = Modifier.padding(12.dp),
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Recent institutions
                            listOf(
                                "Cruz Vermelha - Há 30 minutos",
                                "Banco Alimentar - Há 2 horas",
                                "Caritas Portugal - Há 3 dias"
                            ).forEach { institution ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = institution,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = BorderColor
                            )
                            
                            // All institutions section
                            Text(
                                text = "Todas as Instituições:",
                                modifier = Modifier.padding(12.dp),
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            listOf(
                                "Santa Casa da Misericórdia",
                                "Unicef Portugal",
                                "Fundação AMI"
                            ).forEach { name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
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
