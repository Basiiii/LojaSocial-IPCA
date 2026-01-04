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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lojasocial.app.R
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import com.lojasocial.app.ui.theme.ScanBlue
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.viewmodel.ScanStockViewModel
import com.lojasocial.app.viewmodel.ScanStockUiState
import kotlinx.coroutines.launch
import java.util.Calendar

// Barcode processing function
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (String?) -> Unit
) {
    Log.d("BarcodeScanner", "Processing image proxy: ${imageProxy.width}x${imageProxy.height}, rotation: ${imageProxy.imageInfo.rotationDegrees}")
    
    val image = InputImage.fromMediaImage(
        imageProxy.image!!,
        imageProxy.imageInfo.rotationDegrees
    )

    Log.d("BarcodeScanner", "Created InputImage for barcode scanning")

    barcodeScanner.process(image)
        .addOnSuccessListener { barcodes ->
            Log.d("BarcodeScanner", "Barcode scanning completed. Found ${barcodes.size} barcodes")
            
            for (barcode in barcodes) {
                Log.d("BarcodeScanner", "Barcode detected: format=${barcode.format}, rawValue=${barcode.rawValue}")
                barcode.rawValue?.let { value ->
                    Log.d("BarcodeScanner", "SUCCESS: Barcode value detected: $value")
                    onBarcodeDetected(value)
                    return@addOnSuccessListener
                }
            }
            
            if (barcodes.isEmpty()) {
                Log.d("BarcodeScanner", "No barcodes found in this frame")
            }
        }
        .addOnFailureListener { e ->
            Log.e("BarcodeScanner", "ERROR: Barcode scanning failed", e)
        }
        .addOnCompleteListener {
            Log.d("BarcodeScanner", "Image proxy closed")
            imageProxy.close()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanStockScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanStockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Global exception handler to catch silent crashes
    LaunchedEffect(Unit) {
        try {
            Log.d("ScanStockScreen", "Screen initialized successfully")
        } catch (e: Exception) {
            Log.e("ScanStockScreen", "Screen initialization failed", e)
        }
    }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    val uiState by viewModel.uiState.collectAsState()
    val barcode by viewModel.barcode.collectAsState()
    val productName by viewModel.productName.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val campaign by viewModel.campaign.collectAsState()

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            // Show success message (you can use Snackbar, Toast, etc.)
            Log.d("ScanStock", message)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show error message
            Log.e("ScanStock", error)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Adicionar ao Stock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (hasCameraPermission) {
                CameraSection(
                    onBarcodeDetected = { barcode ->
                        viewModel.onBarcodeScanned(barcode)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Permissão de câmara necessária", color = Color.White)
                }
            }

            FormSection(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}

@Composable
fun CameraSection(
    onBarcodeDetected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastDetectionTime by remember { mutableStateOf(0L) }

    // ML Kit Barcode Scanner
    val barcodeScanner = remember {
        Log.d("CameraSection", "Initializing ML Kit Barcode Scanner")
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        Log.d("CameraSection", "Barcode scanner initialized with formats: EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, QR_CODE")
        scanner
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color.Black)
    ) {
        // 1. Camera Preview with Barcode Scanning
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    // Set up barcode analysis
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        try {
                            processImageProxy(barcodeScanner, imageProxy) { barcode ->
                                barcode?.let {
                                    val currentTime = System.currentTimeMillis()
                                    // Prevent duplicate detections within 2 seconds
                                    if (currentTime - lastDetectionTime > 2000) {
                                        Log.d("CameraSection", " BARCODE DETECTED: $it")
                                        try {
                                            scannedBarcode = it
                                            lastDetectionTime = currentTime
                                            onBarcodeDetected(it)
                                        } catch (e: Exception) {
                                            Log.e("CameraSection", "Error in onBarcodeDetected callback", e)
                                        }
                                    } else {
                                        Log.d("CameraSection", " Duplicate detection ignored: $it")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CameraSection", "Error in image analysis", e)
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxWidth().height(110.dp)
        )

        // 2. Overlay (UI sobre a câmara)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botão Flash (Canto Superior Direito)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    }
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Moldura do Scanner
            Box(
                modifier = Modifier
                    .size(280.dp, 160.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerSize = 20.dp.toPx()

                    // Cantos Azuis
                    val blueColor = ScanBlue

                    // Canto Superior Esquerdo
                    drawLine(blueColor, Offset(0f, 0f), Offset(cornerSize, 0f), strokeWidth)
                    drawLine(blueColor, Offset(0f, 0f), Offset(0f, cornerSize), strokeWidth)

                    // Canto Superior Direito
                    drawLine(blueColor, Offset(size.width, 0f), Offset(size.width - cornerSize, 0f), strokeWidth)
                    drawLine(blueColor, Offset(size.width, 0f), Offset(size.width, cornerSize), strokeWidth)

                    // Canto Inferior Esquerdo
                    drawLine(blueColor, Offset(0f, size.height), Offset(0f, size.height - cornerSize), strokeWidth)
                    drawLine(blueColor, Offset(0f, size.height), Offset(cornerSize, size.height), strokeWidth)

                    // Canto Inferior Direito
                    drawLine(blueColor, Offset(size.width, size.height), Offset(size.width - cornerSize, size.height), strokeWidth)
                    drawLine(blueColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerSize), strokeWidth)

                    // Borda Branca fina à volta (opcional, para dar destaque)
                    drawRoundRect(
                        color = Color.White,
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )

                    // Linha Vermelha de Scan
                    drawLine(
                        color = ScanRed,
                        start = Offset(10f, size.height / 2),
                        end = Offset(size.width - 10f, size.height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Coloca o código barras na zona indicada",
                color = Color.White,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão "Introduz manualmente"
            Button(
                onClick = { /* Lógica para focar no input */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)), // Dark Gray
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Introduz manualmente", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FormSection(
    viewModel: ScanStockViewModel,
    uiState: ScanStockUiState
) {
    Log.d("AddStockView", "FormSection is being composed!")
    val barcode by viewModel.barcode.collectAsState()
    val productName by viewModel.productName.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val campaign by viewModel.campaign.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()
    
    Log.d("AddStockView", "Campaigns count: ${campaigns.size}")
    
    // Date picker state
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Show date picker
    if (showDatePicker) {
        android.app.DatePickerDialog(
            context,
            R.style.CustomDatePickerDialog,
            { _, year: Int, month: Int, day: Int ->
                val formattedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                viewModel.onExpiryDateChanged(formattedDate)
                showDatePicker = false
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Campo Código de Barras
        InputLabel("Código Barras")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcode ?: "",
                onValueChange = { viewModel.onBarcodeChanged(it ?: "") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Faz scan ou introduz código barras", color = TextGray, fontSize = 14.sp) },
                trailingIcon = {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = ScanBlue)
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = ScanBlue
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.fetchProductDataForCurrentBarcode() },
                enabled = !uiState.isLoading && (barcode?.isNotEmpty() == true),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Obter Dados do Produto", color = Color.White, fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo Nome Produto
        InputLabel("Nome Produto")
        OutlinedTextField(
            value = productName ?: "",
            onValueChange = { viewModel.onProductNameChanged(it ?: "") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Introduz nome produto", color = TextGray, fontSize = 14.sp) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = ScanBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo Quantidade
        InputLabel("Quantidade")
        OutlinedTextField(
            value = quantity ?: "",
            onValueChange = { viewModel.onQuantityChanged(it ?: "") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = ScanBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo Data Validade
        InputLabel("Data Validade")
        OutlinedTextField(
            value = expiryDate ?: "mm/dd/aaaa",
            onValueChange = { viewModel.onExpiryDateChanged(it ?: "") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Selecionar Data",
                        tint = ScanBlue
                    )
                }
            },
            placeholder = { Text("Selecionar data", color = TextGray, fontSize = 14.sp) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = ScanBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo Campanha
        InputLabel("Campanha (Opcional)")
        var expanded by remember { mutableStateOf(false) }
        
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = campaign ?: "",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                placeholder = { Text("Selecionar campanha", color = TextGray, fontSize = 14.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = ScanBlue
                )
            )
            
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                campaigns.forEach { campaign ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = campaign.name,
                                fontSize = 14.sp
                            ) 
                        },
                        onClick = {
                            viewModel.onCampaignSelected(campaign)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ScanBlue)
            }
        }

        // Success/Error messages
        uiState.successMessage?.let { message: String ->
            Text(
                text = message,
                color = Color.Green,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        uiState.error?.let { error: String ->
            Text(
                text = error,
                color = Color.Red,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Botão Adicionar Stock
        Button(
            onClick = { 
                viewModel.addToStock()
                viewModel.clearError()
            },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
        ) {
            Text("Adicionar ao Stock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = TextDark,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}