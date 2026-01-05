package com.lojasocial.app.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lojasocial.app.R
import java.util.Calendar
import java.util.Date
import java.util.concurrent.Executors
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import com.lojasocial.app.ui.theme.ScanBlue
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.viewmodel.AddStockViewModel
import com.lojasocial.app.viewmodel.AddStockUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddStockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // State for step navigation
    var currentStep by remember { mutableStateOf("scan") } // "scan" or "form"
    
    // State for manual product addition
    var isManualAddition by remember { mutableStateOf(false) }
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    val productData by viewModel.productData.collectAsState()
    
    // Debug isLoading changes
    LaunchedEffect(uiState.isLoading) {
        Log.e("AddStockView_DEBUG", "=== ISLOADING STATE CHANGED ===")
        Log.e("AddStockView_DEBUG", "New isLoading: ${uiState.isLoading}")
        Log.e("AddStockView_DEBUG", "Manual mode: ${viewModel.isManualMode.value}")
    }
    
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
    
    // Show success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Log.d("ScanStock", message)
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
            },
            onBackToHome = onNavigateBack
        )
    }
}

@Composable
fun ScanStepScreen(
    onNavigateBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
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
                onFlashToggle = { isFlashOn = !isFlashOn }
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
        
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
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
        
        // Scanning overlay
        ScanningOverlay()
        
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
                Text(
                    text = "Adicionar manualmente",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                
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
    onBackToHome: () -> Unit
) {
    Log.e("AddStockView_DEBUG", "=== FORM STEP SCREEN CALLED ===")
    Log.e("AddStockView_DEBUG", "isLoading: ${uiState.isLoading}")
    Log.e("AddStockView_DEBUG", "isManualMode: ${viewModel.isManualMode.value}")
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Form fields
    val barcode by viewModel.barcode.collectAsState()
    val productName by viewModel.productName.collectAsState()
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
                    imageVector = Icons.Default.ArrowBack,
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
        
        // Product info card with editable name
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
                
                // Product image placeholder (will be replaced with actual image later)
                if (productData?.imageUrl?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Imagem do Produto",
                            color = TextGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
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
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
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
            enabled = !uiState.isLoading && quantity.isNotEmpty() && quantity.toIntOrNull() ?: 0 > 0
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
    onFlashToggle: () -> Unit
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
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val scanBoxSize = minOf(canvasWidth, canvasHeight) * 0.6f
        val left = (canvasWidth - scanBoxSize) / 2
        val top = (canvasHeight - scanBoxSize) / 2
        
        // Draw semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )
        
        // Draw transparent scan box
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(scanBoxSize, scanBoxSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw corner brackets
        val bracketLength = 32.dp.toPx()
        val bracketWidth = 4.dp.toPx()
        
        // Top-left corner
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(bracketLength, bracketWidth),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(bracketWidth, bracketLength),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        
        // Top-right corner
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left + scanBoxSize - bracketLength, top),
            size = androidx.compose.ui.geometry.Size(bracketLength, bracketWidth),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left + scanBoxSize - bracketWidth, top),
            size = androidx.compose.ui.geometry.Size(bracketWidth, bracketLength),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        
        // Bottom-left corner
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left, top + scanBoxSize - bracketWidth),
            size = androidx.compose.ui.geometry.Size(bracketLength, bracketWidth),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left, top + scanBoxSize - bracketLength),
            size = androidx.compose.ui.geometry.Size(bracketWidth, bracketLength),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        
        // Bottom-right corner
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left + scanBoxSize - bracketLength, top + scanBoxSize - bracketWidth),
            size = androidx.compose.ui.geometry.Size(bracketLength, bracketWidth),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = ScanBlue,
            topLeft = Offset(left + scanBoxSize - bracketWidth, top + scanBoxSize - bracketLength),
            size = androidx.compose.ui.geometry.Size(bracketWidth, bracketLength),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun DatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var currentDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
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
                    modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Mês Anterior")
                    }
                    
                    Text(
                        text = "${getMonthName(currentMonth)} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Próximo Mês")
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
                var currentWeek = mutableListOf<Int>()
                
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day == 0) {
                                    // Empty cell
                                    Spacer(modifier = Modifier.size(32.dp))
                                } else {
                                    TextButton(
                                        onClick = {
                                            currentDay = day
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (day == currentDay) 
                                                MaterialTheme.colorScheme.primary 
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
