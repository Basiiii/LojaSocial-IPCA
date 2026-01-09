package com.lojasocial.app.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.ScanBlue
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.viewmodel.DeleteStockViewModel
import com.lojasocial.app.viewmodel.DeleteStockUiState
import com.lojasocial.app.domain.product.ProductCategory
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lojasocial.app.ui.components.CustomDatePickerDialog

@Composable
fun DeleteStockScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeleteStockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // State for step navigation
    var currentStep by remember { mutableStateOf("scan") } // "scan" or "form"
    
    // State for manual product addition
    var isManualAddition by remember { mutableStateOf(false) }
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    val stockItemData by viewModel.stockItemData.collectAsState()
    
    // Auto-advance to form when stock item is successfully loaded or manual entry 
    LaunchedEffect(stockItemData, isManualAddition) {
        if (currentStep == "scan" && (stockItemData != null || isManualAddition)) {
            currentStep = "form"
        }
    }
    
    // Handle barcode scanning
    LaunchedEffect(Unit) {
        try {
            Log.d("DeleteStockScreen", "Screen initialized successfully")
        } catch (e: Exception) {
            Log.e("DeleteStockScreen", "Screen initialization failed", e)
        }
    }
    
    // Barcode scanning handler
    fun onBarcodeScanned(barcode: String) {
        Log.d("DeleteStockView", "onBarcodeScanned called with: $barcode")
        if (barcode == "MANUAL") {
            // Manual entry - don't fetch stock data
            Log.d("DeleteStockView", "Switching to manual mode from scanner")
            isManualAddition = true
            viewModel.setManualMode(true)
            viewModel.onBarcodeChanged("")
            viewModel.onProductNameChanged("")
            currentStep = "form"
        } else {
            Log.d("DeleteStockView", "Handling real scanned barcode")
            isManualAddition = false
            viewModel.setManualMode(false)
            viewModel.onBarcodeChanged(barcode)
            currentStep = "form"
            viewModel.onBarcodeScanned(barcode)
        }
    }
    
    // Auto-advance to form when stock item is successfully loaded
    LaunchedEffect(stockItemData) {
        if (stockItemData != null && currentStep == "scan") {
            currentStep = "form"
        }
    }
    
    // Show success message and return to scan screen after deleting product
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Log.d("DeleteStock", message)

            Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.BOTTOM, 0, 150)
            }.show()
            currentStep = "scan"
            isManualAddition = false
            viewModel.setManualMode(false)
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Log.e("DeleteStock", error)
        }
    }
    
    if (currentStep == "scan") {
        // Fullscreen scanning view
        DeleteScanStepScreen(
            onNavigateBack = onNavigateBack,
            onBarcodeScanned = { barcode: String ->
                onBarcodeScanned(barcode)
            }
        )
    } else {
        // Form view
        FormStepScreen(
            viewModel = viewModel,
            uiState = uiState,
            stockItemData = stockItemData,
            onNavigateBack = { 
                currentStep = "scan"
                isManualAddition = false
            }
        )
    }
}

@Composable
fun DeleteScanStepScreen(
    onNavigateBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Camera and barcode scanning state
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Log.e("DeleteScanStepScreen", "Camera permission denied")
            }
        }
    )
    
    // Check camera permission
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        if (hasCameraPermission) {
            CameraPreview(
                onBarcodeDetected = onBarcodeScanned,
                isFlashOn = isFlashOn,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Camera Required",
                        modifier = Modifier.size(64.dp),
                        tint = ScanRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permissão de câmara necessária",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDark
                    )
                }
            }
        }
        
        // Scanning overlay
        DeleteScanningOverlay()
        
        // Header with back button 
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    Log.d("DeleteScanStepScreen", "Back button clicked")
                    onNavigateBack()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            IconButton(
                onClick = { isFlashOn = !isFlashOn },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
        }
        
        // Manual Entry Button
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {                
                Button(
                    onClick = { onBarcodeScanned("MANUAL") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScanRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Eliminar Produto Manualmente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteScanningOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent dark overlay background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )
        }
        
        // Scan box in center
        Box(
            modifier = Modifier.size(280.dp, 160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val cornerSize = 20.dp.toPx()

                // Red Corners for deletion
                val redColor = ScanRed

                // Top Left Corner
                drawLine(
                    color = redColor,
                    start = Offset(0f, 0f),
                    end = Offset(cornerSize, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = redColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, cornerSize),
                    strokeWidth = strokeWidth
                )

                // Top Right Corner
                drawLine(
                    color = redColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width - cornerSize, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = redColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, cornerSize),
                    strokeWidth = strokeWidth
                )

                // Bottom Left Corner
                drawLine(
                    color = redColor,
                    start = Offset(0f, size.height),
                    end = Offset(0f, size.height - cornerSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = redColor,
                    start = Offset(0f, size.height),
                    end = Offset(cornerSize, size.height),
                    strokeWidth = strokeWidth
                )

                // Bottom Right Corner
                drawLine(
                    color = redColor,
                    start = Offset(size.width, size.height),
                    end = Offset(size.width - cornerSize, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = redColor,
                    start = Offset(size.width, size.height),
                    end = Offset(size.width, size.height - cornerSize),
                    strokeWidth = strokeWidth
                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormStepScreen(
    viewModel: DeleteStockViewModel,
    uiState: DeleteStockUiState,
    stockItemData: com.lojasocial.app.domain.stock.StockItem?,
    onNavigateBack: () -> Unit,
) {
    Log.e("DeleteStockView_DEBUG", "=== FORM STEP SCREEN CALLED ===")
    Log.e("DeleteStockView_DEBUG", "isLoading: ${uiState.isLoading}")
    Log.e("DeleteStockView_DEBUG", "isManualMode: ${viewModel.isManualMode.collectAsState().value}")
    
    // Form fields
    val barcode by viewModel.barcode.collectAsState()
    val productName by viewModel.productName.collectAsState()
    val productBrand by viewModel.productBrand.collectAsState()
    val productCategory by viewModel.productCategory.collectAsState()
    val productImageUrl by viewModel.productImageUrl.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val campaign by viewModel.campaign.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()
    val isManualMode by viewModel.isManualMode.collectAsState()
    
    // Expiry date toggle state
    var expiryDateEnabled by remember { mutableStateOf(true) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Campaign dropdown state
    var campaignExpanded by remember { mutableStateOf(false) }
    
    // Category dropdown state
    var categoryExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Scanner"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Eliminar Stock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Product info card with read-only fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScanRed.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Produto a Eliminar",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScanRed
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Product image - always show, use default if no URL available
                val imageUrlToShow = when {
                    productImageUrl.isNotEmpty() -> productImageUrl
                    else -> AppConstants.DEFAULT_PRODUCT_IMAGE_URL
                }
                
                AsyncImage(
                    model = imageUrlToShow,
                    contentDescription = "Imagem do Produto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Read-only product name
                OutlinedTextField(
                    value = productName,
                    onValueChange = {},
                    label = { Text("Nome do Produto") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    readOnly = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode input (read-only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {},
                        label = { Text("Código de Barras") },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        readOnly = true
                    )

                    Button(
                        onClick = { 
                            if (barcode.isNotEmpty()) {
                                viewModel.fetchStockDataForCurrentBarcode()
                            }
                        },
                        enabled = !uiState.isLoading && barcode.isNotEmpty(),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Procurar")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brand field (read-only)
        OutlinedTextField(
            value = productBrand,
            onValueChange = {},
            label = { Text("Marca") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category dropdown (read-only)
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = ProductCategory.fromId(productCategory)?.displayName ?: "Alimentar",
                onValueChange = {},
                label = { Text("Categoria") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = false
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Image URL field (read-only)
        OutlinedTextField(
            value = productImageUrl,
            onValueChange = {},
            label = { Text("URL da Imagem") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quantity field (read-only)
        OutlinedTextField(
            value = quantity,
            onValueChange = {},
            label = { Text("Quantidade em Stock") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expiry date (read-only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Data de Validade",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = expiryDate,
            onValueChange = {},
            label = { Text("Data de Validade") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }, enabled = false) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Selecionar Data"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campaign dropdown (read-only)
        ExposedDropdownMenuBox(
            expanded = campaignExpanded,
            onExpandedChange = { campaignExpanded = !campaignExpanded }
        ) {
            OutlinedTextField(
                value = campaign.ifEmpty { "Nenhuma Campanha" },
                onValueChange = {},
                label = { Text("Campanha") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = campaignExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = false
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Delete from stock button
        Button(
            onClick = { viewModel.deleteFromStock() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && quantity.isNotEmpty() && (quantity.toIntOrNull() ?: 0) > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = ScanRed
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar do Stock")
            }
        }
        
        // Success/Error messages
        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }
        
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ScanRed)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: BarcodeScanner,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
