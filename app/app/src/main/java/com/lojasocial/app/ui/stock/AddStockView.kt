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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Calendar
import java.util.concurrent.Executors
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.ScanBlue
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.viewmodel.AddStockViewModel
import com.lojasocial.app.viewmodel.AddStockUiState
import com.lojasocial.app.data.model.ProductCategory
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun AddStockScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddStockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // State for step navigation
    var currentStep by remember { mutableStateOf("scan") } // "scan" or "form"
    
    // State for manual product addition
    var isManualAddition by remember { mutableStateOf(false) }
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    val productData by viewModel.productData.collectAsState()
    
    // Auto-advance to form when product is successfully loaded or manual entry 
    LaunchedEffect(productData, isManualAddition) {
        if (currentStep == "scan" && (productData != null || isManualAddition)) {
            currentStep = "form"
        }
    }
    
    // Handle barcode scanning
    LaunchedEffect(Unit) {
        try {
            Log.d("AddStockScreen", "Screen initialized successfully")
        } catch (e: Exception) {
            Log.e("AddStockScreen", "Screen initialization failed", e)
        }
    }
    
    // Barcode scanning handler
    fun onBarcodeScanned(barcode: String) {
        Log.d("AddStockView", "onBarcodeScanned called with: $barcode")
        if (barcode == "MANUAL") {
            // Manual entry - don't fetch product data
            Log.d("AddStockView", "Switching to manual mode from scanner")
            isManualAddition = true
            viewModel.setManualMode(true)
            viewModel.onBarcodeChanged("")
            viewModel.onProductNameChanged("")
            currentStep = "form"
        } else {
            Log.d("AddStockView", "Handling real scanned barcode")
            isManualAddition = false
            viewModel.setManualMode(false)
            viewModel.onBarcodeChanged(barcode)
            currentStep = "form"
            viewModel.onBarcodeScanned(barcode)
        }
    }
    
    // Auto-advance to form when product is successfully loaded
    LaunchedEffect(productData) {
        if (productData != null && currentStep == "scan") {
            currentStep = "form"
        }
    }
    
    // Show success message and return to scan screen after adding product
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Log.d("ScanStock", message)
            // Show toast slightly higher than the very bottom
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
            Log.e("ScanStock", error)
        }
    }
    
    if (currentStep == "scan") {
        // Fullscreen scanning view
        ScanStepScreen(
            onNavigateBack = onNavigateBack,
            onBarcodeScanned = { barcode ->
                onBarcodeScanned(barcode)
            }
        )
    } else {
        // Form view
        FormStepScreen(
            viewModel = viewModel,
            uiState = uiState,
            productData = productData,
            onNavigateBack = { 
                currentStep = "scan"
                isManualAddition = false
            }
        )
    }
}

@Composable
fun ScanStepScreen(
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
                Log.e("ScanStepScreen", "Camera permission denied")
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
                        tint = ScanBlue
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
        ScanningOverlay()
        
        // Header with back button 
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    Log.d("ScanStepScreen", "Back button clicked")
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
                        containerColor = LojaSocialPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Adicionar Produto Manualmente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormStepScreen(
    viewModel: AddStockViewModel,
    uiState: AddStockUiState,
    productData: com.lojasocial.app.data.model.Product?,
    onNavigateBack: () -> Unit,
) {
    Log.e("AddStockView_DEBUG", "=== FORM STEP SCREEN CALLED ===")
    Log.e("AddStockView_DEBUG", "isLoading: ${uiState.isLoading}")
    Log.e("AddStockView_DEBUG", "isManualMode: ${viewModel.isManualMode.collectAsState().value}")
    
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
                text = "Adicionar Stock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Product info card with editable fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ScanBlue.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Produto",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScanBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Product image - always show, use default if no URL available
                val defaultImageUrl = "https://drive.google.com/uc?export=view&id=1pFBQEmEMZOnUoDeQxus054ezCihRywPQ"
                val imageUrlToShow = when {
                    productImageUrl.isNotEmpty() -> productImageUrl
                    !productData?.imageUrl.isNullOrEmpty() -> productData?.imageUrl
                    else -> defaultImageUrl
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
                
                // Editable product name
                OutlinedTextField(
                    value = productName,
                    onValueChange = { viewModel.onProductNameChanged(it) },
                    label = { Text("Nome do Produto") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // In manual mode, always keep the barcode as an editable field
                    if (isManualMode) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { viewModel.onBarcodeChanged(it) },
                            label = { Text("Código de Barras") },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            placeholder = { Text("Digite o código de barras") }
                        )
                    } else {
                        // In scan mode, show the scanned barcode as read-only text
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = {},
                            label = { Text("Código de Barras") },
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                    }

                    Button(
                        onClick = { 
                            if (barcode.isNotEmpty()) {
                                viewModel.fetchProductDataForCurrentBarcode()
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
        
        // Brand field
        OutlinedTextField(
            value = productBrand,
            onValueChange = { viewModel.onProductBrandChanged(it) },
            label = { Text("Marca") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category dropdown
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
                enabled = !uiState.isLoading
            )
            
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                ProductCategory.getAllCategories().forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.displayName) },
                        onClick = {
                            viewModel.onProductCategoryChanged(category.id)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Image URL field
        OutlinedTextField(
            value = productImageUrl,
            onValueChange = { viewModel.onProductImageUrlChanged(it) },
            label = { Text("URL da Imagem") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            placeholder = { Text("https://exemplo.com/imagem.jpg") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quantity field
        OutlinedTextField(
            value = quantity,
            onValueChange = { viewModel.onQuantityChanged(it) },
            label = { Text("Quantidade") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expiry date toggle and field
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
            
            // Toggle switch
            Switch(
                checked = expiryDateEnabled,
                onCheckedChange = { expiryDateEnabled = it },
                enabled = !uiState.isLoading
            )
        }
        
        if (expiryDateEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { viewModel.onExpiryDateChanged(it) },
                label = { Text("Data de Validade (DD/MM/AAAA)") },
                placeholder = { Text("DD/MM/AAAA") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Selecionar Data"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            
            // Date Picker Dialog
            if (showDatePicker) {
                DatePickerDialog(
                    onDateSelected = { day, month, year ->
                        val formattedDate = String.format("%02d/%02d/%04d", day, month, year)
                        viewModel.onExpiryDateChanged(formattedDate)
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campaign dropdown
        ExposedDropdownMenuBox(
            expanded = campaignExpanded,
            onExpandedChange = { campaignExpanded = !campaignExpanded }
        ) {
            OutlinedTextField(
                value = campaign,
                onValueChange = { viewModel.onCampaignChanged(it) },
                label = { Text("Campanha (Opcional)") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = campaignExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !uiState.isLoading
            )
            
            ExposedDropdownMenu(
                expanded = campaignExpanded,
                onDismissRequest = { campaignExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Nenhuma Campanha") },
                    onClick = {
                        viewModel.onCampaignChanged("")
                        campaignExpanded = false
                    }
                )
                
                campaigns.forEach { campaign ->
                    DropdownMenuItem(
                        text = { Text(campaign.name) },
                        onClick = {
                            viewModel.onCampaignSelected(campaign)
                            campaignExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Add to stock button
        Button(
            onClick = { 
                if (!expiryDateEnabled) {
                    // Set a default expiry date when disabled
                    viewModel.onExpiryDateChanged("Sem Validade")
                }
                viewModel.addToStock() 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && quantity.isNotEmpty() && (quantity.toIntOrNull()
                ?: 0) > 0
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Adicionar ao Stock")
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

@Composable
fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    isFlashOn: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A)
                .build()
        )
    }
    
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    
    LaunchedEffect(isFlashOn) {
        cameraControl?.enableTorch(isFlashOn)
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            processImageProxy(imageProxy, barcodeScanner, onBarcodeDetected)
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalyzer
                    )
                    
                    cameraControl = camera.cameraControl
                    
                    camera.cameraControl.enableTorch(isFlashOn)
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
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

@Composable
fun ScanningOverlay() {
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

                // Blue Corners
                val blueColor = ScanBlue

                // Top Left Corner
                drawLine(
                    color = blueColor,
                    start = Offset(0f, 0f),
                    end = Offset(cornerSize, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = blueColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, cornerSize),
                    strokeWidth = strokeWidth
                )

                // Top Right Corner
                drawLine(
                    color = blueColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width - cornerSize, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = blueColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, cornerSize),
                    strokeWidth = strokeWidth
                )

                // Bottom Left Corner
                drawLine(
                    color = blueColor,
                    start = Offset(0f, size.height),
                    end = Offset(0f, size.height - cornerSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = blueColor,
                    start = Offset(0f, size.height),
                    end = Offset(cornerSize, size.height),
                    strokeWidth = strokeWidth
                )

                // Bottom Right Corner
                drawLine(
                    color = blueColor,
                    start = Offset(size.width, size.height),
                    end = Offset(size.width - cornerSize, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = blueColor,
                    start = Offset(size.width, size.height),
                    end = Offset(size.width, size.height - cornerSize),
                    strokeWidth = strokeWidth
                )

            }
        }
    }
}

@Composable
fun DatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var currentDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Selecionar Data de Validade",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Month/Year selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LojaSocialPrimary),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mês Anterior",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "${getMonthName(currentMonth)} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Próximo Mês",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days of week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Calendar grid
                val daysInMonth = getDaysInMonth(currentMonth, currentYear)
                val firstDayOfWeek = getFirstDayOfMonth(currentMonth, currentYear)
                
                // Generate calendar weeks
                val calendarDays = mutableListOf<List<Int>>()
                val currentWeek = mutableListOf<Int>()
                
                // Add empty cells for days before month starts
                repeat(firstDayOfWeek) {
                    currentWeek.add(0)
                }
                
                // Add days of the month
                for (day in 1..daysInMonth) {
                    currentWeek.add(day)
                    if (currentWeek.size == 7) {
                        calendarDays.add(currentWeek.toList())
                        currentWeek.clear()
                    }
                }
                
                // Add remaining days
                if (currentWeek.isNotEmpty()) {
                    while (currentWeek.size < 7) {
                        currentWeek.add(0)
                    }
                    calendarDays.add(currentWeek.toList())
                }
                
                // Display calendar weeks
                calendarDays.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { day ->
                            if (day == 0) {
                                // Empty cell to align days
                                Spacer(modifier = Modifier.size(40.dp))
                            } else {
                                TextButton(
                                    onClick = { currentDay = day },
                                    modifier = Modifier.size(40.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (day == currentDay)
                                            LojaSocialPrimary.copy(alpha = 0.2f)
                                        else
                                            Color.Transparent,
                                        contentColor = if (day == currentDay)
                                            LojaSocialPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(currentDay, currentMonth + 1, currentYear) // +1 because month is 0-based
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun getMonthName(month: Int): String {
    return when (month) {
        0 -> "Janeiro"
        1 -> "Fevereiro"
        2 -> "Março"
        3 -> "Abril"
        4 -> "Maio"
        5 -> "Junho"
        6 -> "Julho"
        7 -> "Agosto"
        8 -> "Setembro"
        9 -> "Outubro"
        10 -> "Novembro"
        11 -> "Dezembro"
        else -> ""
    }
}

private fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        0, 2, 4, 6, 7, 9, 11 -> 31
        1 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

private fun getFirstDayOfMonth(month: Int, year: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0-based (0 = Sunday)
}

private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
