package com.lojasocial.app.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.utils.AppConstants

enum class StockStatus(val label: String, val color: Color, val bgColor: Color) {
    IN_STOCK("Em stock", Color(0xFF1B5E20), Color(0xFFDFF7E2)),
    LOW_STOCK("Stock baixo", Color(0xFFC86E39), Color(0xFFFCECDD)),
    OUT_OF_STOCK("Esgotado", Color(0xFFD32F2F), Color(0xFFFFDAD4))
}

fun getStockStatus(quantity: Int): StockStatus {
    return when {
        quantity == 0 -> StockStatus.OUT_OF_STOCK
        quantity <= 5 -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListView(
    onNavigateBack: () -> Unit,
    onNavigateToStockItems: (String) -> Unit,
    viewModel: StockListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Stock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                val errorMessage = uiState.error
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Tentar novamente")
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    StockListFilterHeader(
                        productCount = uiState.filteredProducts.size,
                        onFilterClick = { },
                        viewModel = viewModel
                    )

                    if (uiState.products.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum produto em stock",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.filteredProducts) { productWithStock ->
                                ProductCard(
                                    productWithStock = productWithStock,
                                    onNavigateClick = { onNavigateToStockItems(productWithStock.product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockListFilterHeader(
    productCount: Int,
    onFilterClick: () -> Unit,
    viewModel: StockListViewModel
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategories = uiState.selectedCategories

    // App Blue Color (extracted for reuse within this scope)
    val accentColor = Color(0xFF2D75F0)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$productCount Produtos",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // Filter Button Chip
            Box {
                Surface(
                    onClick = { showFilterMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedCategories.isNotEmpty()) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtrar",
                            color = accentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Modern Minimalist Dropdown
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier
                        .width(260.dp)
                        .background(Color.White),
                    shape = RoundedCornerShape(16.dp), // Modern rounded corners
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    // Header
                    Text(
                        text = "Filtrar por Categoria",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    // Categories
                    ProductCategory.values().forEach { category ->
                        val categoryName = when (category) {
                            ProductCategory.ALIMENTAR -> "Alimentar"
                            ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                            ProductCategory.CASA -> "Limpeza"
                        }
                        val categoryIcon = when (category) {
                            ProductCategory.ALIMENTAR -> Icons.Default.Restaurant
                            ProductCategory.HIGIENE_PESSOAL -> Icons.Default.Spa
                            ProductCategory.CASA -> Icons.Default.Home
                        }
                        val isSelected = selectedCategories.contains(category)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                }
                            },
                            onClick = { viewModel.toggleCategory(category) },
                            colors = MenuDefaults.itemColors(
                                textColor = if (isSelected) accentColor else Color.Black,
                                leadingIconColor = if (isSelected) accentColor else Color.Gray,
                                trailingIconColor = accentColor,
                            ),
                            modifier = Modifier.background(
                                if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent
                            )
                        )
                    }

                    // Reset Filter Action
                    if (selectedCategories.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFFF0F0F0)
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Limpar filtros",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                viewModel.clearFilters()
                                showFilterMenu = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.Red.copy(alpha = 0.8f),
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
    }
}

@Composable
fun ProductCard(
    productWithStock: ProductWithStock,
    onNavigateClick: () -> Unit
) {
    val status = getStockStatus(productWithStock.totalStock)
    val category = ProductCategory.fromId(productWithStock.product.category)
    val categoryIcon = when (category) {
        ProductCategory.ALIMENTAR -> Icons.Default.Restaurant
        ProductCategory.HIGIENE_PESSOAL -> Icons.Default.Spa
        ProductCategory.CASA -> Icons.Default.Home
        null -> Icons.Default.ShoppingCart
    }
    val imageUrl = productWithStock.product.imageUrl.ifEmpty { AppConstants.DEFAULT_PRODUCT_IMAGE_URL }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = productWithStock.product.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(categoryIcon)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productWithStock.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (productWithStock.product.brand.isNotEmpty()) {
                    Text(
                        text = productWithStock.product.brand,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = productWithStock.categoryName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = status.bgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${status.label} (${productWithStock.totalStock})",
                        color = status.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onNavigateClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver itens",
                    tint = Color(0xFF2D75F0),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
