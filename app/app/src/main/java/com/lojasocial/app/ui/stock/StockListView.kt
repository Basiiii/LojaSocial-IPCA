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
import androidx.compose.material.icons.filled.Sort
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
import com.lojasocial.app.ui.components.ProductImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.lojasocial.app.ui.theme.LojaSocialPrimary

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
    var showSearchBar by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
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
                    if (showSearchBar) {
                        StockListSearchBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onClose = { showSearchBar = false }
                        )
                    }
                    
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    val sortBy = uiState.sortBy
    val hideNonPerishable = uiState.hideNonPerishable
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

            Box {
                Surface(
                    onClick = { showFilterMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedCategories.isNotEmpty() || hideNonPerishable || sortBy != SortOption.NAME) accentColor.copy(alpha = 0.1f) else Color.Transparent,
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
                            text = "Filtrar & Ordenar",
                            color = accentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier
                        .width(260.dp)
                        .background(Color.White),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "Filtrar por Categoria",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFF0F0F0)
                    )
                    
                    // Sort options
                    Text(
                        text = "Ordenar por",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (sortBy == SortOption.NAME) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Nome",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (sortBy == SortOption.NAME) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sortBy == SortOption.NAME) accentColor else Color.Black
                                )
                            }
                        },
                        onClick = {
                            viewModel.setSortBy(SortOption.NAME)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (sortBy == SortOption.NAME) accentColor else Color.Black
                        ),
                        modifier = Modifier.background(
                            if (sortBy == SortOption.NAME) accentColor.copy(alpha = 0.08f) else Color.Transparent
                        )
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (sortBy == SortOption.EXPIRY_DATE) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Data de Validade",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (sortBy == SortOption.EXPIRY_DATE) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sortBy == SortOption.EXPIRY_DATE) accentColor else Color.Black
                                )
                            }
                        },
                        onClick = {
                            viewModel.setSortBy(SortOption.EXPIRY_DATE)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (sortBy == SortOption.EXPIRY_DATE) accentColor else Color.Black
                        ),
                        modifier = Modifier.background(
                            if (sortBy == SortOption.EXPIRY_DATE) accentColor.copy(alpha = 0.08f) else Color.Transparent
                        )
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFF0F0F0)
                    )
                    
                    // Hide non-perishable option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hideNonPerishable) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Ocultar não perecíveis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (hideNonPerishable) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (hideNonPerishable) accentColor else Color.Black
                                )
                            }
                        },
                        onClick = {
                            viewModel.setHideNonPerishable(!hideNonPerishable)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (hideNonPerishable) accentColor else Color.Black
                        ),
                        modifier = Modifier.background(
                            if (hideNonPerishable) accentColor.copy(alpha = 0.08f) else Color.Transparent
                        )
                    )
                    
                    if (selectedCategories.isNotEmpty() || hideNonPerishable || sortBy != SortOption.NAME) {
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
fun StockListSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Pesquisar produtos...", color = Color.Gray) },
        leadingIcon = { 
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) 
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.Gray)
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedBorderColor = LojaSocialPrimary,
            cursorColor = LojaSocialPrimary
        ),
        singleLine = true
    )
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

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                product = productWithStock.product,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Main Text Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productWithStock.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    lineHeight = 16.sp
                )

                // Increased gap between Name and Brand
                Spacer(modifier = Modifier.height(4.dp))

                if (productWithStock.product.brand.isNotEmpty()) {
                    Text(
                        text = productWithStock.product.brand,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
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
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stock Label (Moved to Right)
            Surface(
                color = status.bgColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${status.label} (${productWithStock.totalStock})",
                    color = status.color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
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