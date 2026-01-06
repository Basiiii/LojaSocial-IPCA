package com.lojasocial.app.ui.expiringitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.data.model.StockItem
import com.lojasocial.app.domain.ExpiringItemWithProduct
import com.lojasocial.app.domain.ExpiringItemsUiState
import com.lojasocial.app.ui.expiringitems.components.*
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialTheme
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
 * @param viewModel The view model managing the expiring items state and business logic
 * 
 * @see ExpiringItemsViewModel The ViewModel managing the state
 * @see ExpiringItemCard Component for displaying individual expiring items
 */
@Composable
fun ExpiringItemsView(
    onNavigateBack: () -> Unit,
    viewModel: ExpiringItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
    ) {
        ExpiringItemsTopBar(onNavigateBack = onNavigateBack)

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
            ExpiringItemsTopBar(onNavigateBack = {})
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
            ExpiringItemsTopBar(onNavigateBack = {})
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
            ExpiringItemsTopBar(onNavigateBack = {})
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
            ExpiringItemsTopBar(onNavigateBack = {})
            ExpiringItemsEmptyState()
        }
    }
}
