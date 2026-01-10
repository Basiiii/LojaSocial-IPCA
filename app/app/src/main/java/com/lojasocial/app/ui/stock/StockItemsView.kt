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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.lojasocial.app.utils.FileUtils
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.domain.stock.StockItem
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.components.ProductImage
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
    val context = LocalContext.current

    LaunchedEffect(barcode) {
        viewModel.loadStockItems(barcode)
    }
    
    // Decode product image once (since all items share the same product)
    // Only decode if there's no imageUrl (imageUrl is faster)
    val hasImageUrl = !uiState.product?.imageUrl.isNullOrEmpty()
    var decodedProductImage by remember(uiState.product?.serializedImage, hasImageUrl) { 
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) 
    }
    
    LaunchedEffect(uiState.product?.serializedImage, hasImageUrl) {
        // Skip Base64 decoding if imageUrl exists (it's faster)
        if (!hasImageUrl) {
            decodedProductImage = null // Reset when product changes
            uiState.product?.serializedImage?.let { base64 ->
                if (base64.isNotBlank()) {
                    // Decode on background thread
                    decodedProductImage = withContext(Dispatchers.IO) {
                        try {
                            val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                            bytes?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        } else {
            // Clear decoded image if imageUrl is available
            decodedProductImage = null
        }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing between cards slightly
                ) {
                    items(
                        items = uiState.items,
                        key = { it.stockItem.id }
                    ) { itemWithProduct ->
                        StockItemCard(
                            itemWithProduct = itemWithProduct,
                            viewModel = viewModel,
                            preDecodedImage = decodedProductImage
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
    viewModel: StockItemsViewModel,
    preDecodedImage: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    var showCampaignDialog by remember { mutableStateOf(false) }
    
    StockItemCardContent(
        itemWithProduct = itemWithProduct,
        onCampaignInfoClick = {
            itemWithProduct.stockItem.campaignId?.let { campaignId ->
                showCampaignDialog = true
            }
        },
        preDecodedImage = preDecodedImage
    )
    
    if (showCampaignDialog && itemWithProduct.stockItem.campaignId != null) {
        val campaignId = itemWithProduct.stockItem.campaignId!!
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

// Shared date formatter to avoid creating it for each card
private val sharedDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@Composable
private fun StockItemCardContent(
    itemWithProduct: StockItemWithProduct,
    onCampaignInfoClick: () -> Unit = {},
    preDecodedImage: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    val hasCampaign = itemWithProduct.stockItem.campaignId != null
    
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
                product = itemWithProduct.product,
                preDecodedBitmap = preDecodedImage,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Middle Column: Text Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quantidade: ${itemWithProduct.stockItem.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    lineHeight = 16.sp
                )
                
                // Minimized Spacer
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Validade Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Validade",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = itemWithProduct.stockItem.expirationDate?.let { sharedDateFormat.format(it) } ?: "Sem data",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 12.sp
                        )
                    }
                    
                    // Adicionado Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adicionado",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = sharedDateFormat.format(itemWithProduct.stockItem.createdAt),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            // Info Icon (Right)
            if (hasCampaign) {
                IconButton(
                    onClick = onCampaignInfoClick,
                    modifier = Modifier.size(32.dp)
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

    LaunchedEffect(campaignId) {
        if (campaignId.isNotEmpty()) {
            viewModel.loadCampaign(campaignId)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    text = "Detalhes da Campanha",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )

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
                                imageVector = Icons.Default.Info,
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

                        Text(
                            text = "Duração",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Stock Items View")
@Composable
private fun StockItemsViewPreview() {
    val mockProduct = Product(
        id = "123456789",
        name = "Desodorizante Unisexo",
        brand = "Dove",
        category = 3,
        imageUrl = ""
    )
    
    val futureDate = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 30)
    }.time
    val pastDate = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -5)
    }.time
    
    val mockItems = listOf(
        StockItemWithProduct(
            stockItem = StockItem(
                id = "item1",
                barcode = "123456789",
                quantity = 5,
                createdAt = pastDate,
                expirationDate = futureDate,
                campaignId = "Campanha de Verão"
            ),
            product = mockProduct
        ),
        StockItemWithProduct(
            stockItem = StockItem(
                id = "item2",
                barcode = "123456789",
                quantity = 3,
                createdAt = pastDate,
                expirationDate = null,
                campaignId = null
            ),
            product = mockProduct
        ),
        StockItemWithProduct(
            stockItem = StockItem(
                id = "item3",
                barcode = "123456789",
                quantity = 10,
                createdAt = pastDate,
                expirationDate = futureDate,
                campaignId = "Campanha de Inverno"
            ),
            product = mockProduct
        )
    )
    
    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = mockProduct.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mockItems) { itemWithProduct ->
                StockItemCardContent(
                    itemWithProduct = itemWithProduct,
                    onCampaignInfoClick = {}
                )
            }
        }
    }
}