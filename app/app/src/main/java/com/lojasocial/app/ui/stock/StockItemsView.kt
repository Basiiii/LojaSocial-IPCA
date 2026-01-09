package com.lojasocial.app.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockItemsView(
    barcode: String,
    onNavigateBack: () -> Unit,
    viewModel: StockItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(barcode) {
        viewModel.loadStockItems(barcode)
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.product?.name ?: "Itens em Stock",
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
                }
            }
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum item em stock para este produto",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.items) { itemWithProduct ->
                        StockItemCard(
                            itemWithProduct = itemWithProduct,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockItemCard(
    itemWithProduct: StockItemWithProduct,
    viewModel: StockItemsViewModel
) {
    var showCampaignDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val category = itemWithProduct.product?.let { ProductCategory.fromId(it.category) }
    val categoryIcon = when (category) {
        ProductCategory.ALIMENTAR -> Icons.Default.Restaurant
        ProductCategory.HIGIENE_PESSOAL -> Icons.Default.Spa
        ProductCategory.CASA -> Icons.Default.Home
        null -> Icons.Default.ShoppingCart
    }
    val imageUrl = itemWithProduct.product?.imageUrl?.ifEmpty { AppConstants.DEFAULT_PRODUCT_IMAGE_URL } 
        ?: AppConstants.DEFAULT_PRODUCT_IMAGE_URL
    val hasCampaign = itemWithProduct.stockItem.campaignId != null
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = itemWithProduct.product?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(categoryIcon)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quantidade: ${itemWithProduct.stockItem.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Validade",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        itemWithProduct.stockItem.expirationDate?.let { expDate ->
                            Text(
                                text = dateFormat.format(expDate),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        } ?: run {
                            Text(
                                text = "Sem data",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adicionado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = dateFormat.format(itemWithProduct.stockItem.createdAt),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            if (hasCampaign) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        itemWithProduct.stockItem.campaignId?.let { campaignId ->
                            android.util.Log.d("StockItemCard", "Info button clicked for campaignId: $campaignId")
                            showCampaignDialog = true
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ver campanha",
                        tint = Color(0xFF2D75F0),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    if (showCampaignDialog && itemWithProduct.stockItem.campaignId != null) {
        val campaignId = itemWithProduct.stockItem.campaignId!!
        LaunchedEffect(campaignId) {
            android.util.Log.d("StockItemCard", "Dialog shown, loading campaign: $campaignId")
            viewModel.loadCampaign(campaignId)
        }
        CampaignDetailsDialog(
            campaignId = campaignId,
            viewModel = viewModel,
            onDismiss = { 
                showCampaignDialog = false
                viewModel.resetCampaignState()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsDialog(
    campaignId: String,
    viewModel: StockItemsViewModel,
    onDismiss: () -> Unit
) {
    val campaignState by viewModel.campaignState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }

    // Load campaign data
    LaunchedEffect(campaignId) {
        if (campaignId.isNotEmpty()) {
            viewModel.loadCampaign(campaignId)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        // Main Container: A floating Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detalhes da Campanha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.Gray
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color(0xFFF0F0F0)
                )

                when {
                    campaignState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2D75F0))
                        }
                    }
                    campaignState.error != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = campaignState.error ?: "Erro ao carregar",
                                color = Color.Black,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    campaignState.campaign != null -> {
                        val campaign = campaignState.campaign!!

                        // Campaign Name
                        Text(
                            text = "Nome",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = campaign.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dates Section
                        Text(
                            text = "Duração",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.DateRange,
                                contentDescription = null,
                                tint = LojaSocialPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${dateFormat.format(campaign.startDate)} - ${dateFormat.format(campaign.endDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
