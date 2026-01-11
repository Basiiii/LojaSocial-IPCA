package com.lojasocial.app.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.lojasocial.app.ui.components.ProductImage
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.ScanBlue
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.utils.FileUtils
import com.lojasocial.app.viewmodel.AddStockUiState
import com.lojasocial.app.viewmodel.AddStockViewModel
import java.io.File
import java.util.concurrent.Executors

@Composable
fun AddStockScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddStockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf("scan") }
    var isManualAddition by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val productData by viewModel.productData.collectAsState()

    LaunchedEffect(productData, isManualAddition) {
        if (currentStep == "scan" && (productData != null || isManualAddition)) {
            currentStep = "form"
        }
    }

    LaunchedEffect(Unit) {
        try {
            Log.d("AddStockScreen", "Screen initialized successfully")
        } catch (e: Exception) {
            Log.e("AddStockScreen", "Screen initialization failed", e)
        }
    }

    fun onBarcodeScanned(barcode: String) {
        if (barcode == "MANUAL") {
            isManualAddition = true
            viewModel.setManualMode(true)
            viewModel.onBarcodeChanged("")
            viewModel.onProductNameChanged("")
            currentStep = "form"
        } else {
            isManualAddition = false
            viewModel.setManualMode(false)
            viewModel.onBarcodeChanged(barcode)
            currentStep = "form"
            viewModel.onBarcodeScanned(barcode)
        }
    }

    LaunchedEffect(productData) {
        if (productData != null && currentStep == "scan") {
            currentStep = "form"
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.BOTTOM, 0, 150)
            }.show()
            currentStep = "scan"
            isManualAddition = false
            viewModel.setManualMode(false)
        }
    }

    if (currentStep == "scan") {
        ScanStepScreen(
            onNavigateBack = onNavigateBack,
            onBarcodeScanned = { barcode ->
                onBarcodeScanned(barcode)
            }
        )
    } else {
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
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

        ScanningOverlay()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { isFlashOn = !isFlashOn },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onBarcodeScanned("MANUAL") },
                    colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
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
    productData: com.lojasocial.app.domain.product.Product?,
    onNavigateBack: () -> Unit,
) {
    val barcode by viewModel.barcode.collectAsState()
    val productName by viewModel.productName.collectAsState()
    val productBrand by viewModel.productBrand.collectAsState()
    val productCategory by viewModel.productCategory.collectAsState()
    val productImageUrl by viewModel.productImageUrl.collectAsState()
    val productSerializedImage by viewModel.productSerializedImage.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val campaign by viewModel.campaign.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()
    val isManualMode by viewModel.isManualMode.collectAsState()

    val context = LocalContext.current
    var expiryDateEnabled by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var campaignExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isBarcodeEditable by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = FileUtils.convertImageToBase64(
                context = context,
                uri = it,
                filePath = null,
                maxWidth = 800,
                maxHeight = 800,
                quality = 85
            )
            result.fold(
                onSuccess = { base64 ->
                    // Remove background from the image
                    viewModel.removeBackgroundFromImage(
                        imageBase64 = base64,
                        onSuccess = { processedBase64 ->
                            viewModel.onProductSerializedImageChanged(processedBase64)
                            viewModel.onProductImageUrlChanged("")
                            Log.d("AddStockView", "Image processed and background removed")
                        },
                        onFailure = { errorMsg ->
                            // If background removal fails, use original image
                            Log.w("AddStockView", "Background removal failed: $errorMsg, using original image")
                            viewModel.onProductSerializedImageChanged(base64)
                            viewModel.onProductImageUrlChanged("")
                        }
                    )
                },
                onFailure = { error -> Log.e("AddStockView", "Error: ${error.message}") }
            )
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val result = FileUtils.convertImageToBase64(
                    context = context,
                    uri = uri,
                    filePath = null,
                    maxWidth = 800,
                    maxHeight = 800,
                    quality = 85
                )
                result.fold(
                    onSuccess = { base64 ->
                        // Remove background from the image
                        viewModel.removeBackgroundFromImage(
                            imageBase64 = base64,
                            onSuccess = { processedBase64 ->
                                viewModel.onProductSerializedImageChanged(processedBase64)
                                viewModel.onProductImageUrlChanged("")
                                Log.d("AddStockView", "Camera image processed and background removed")
                            },
                            onFailure = { errorMsg ->
                                // If background removal fails, use original image
                                Log.w("AddStockView", "Background removal failed: $errorMsg, using original image")
                                viewModel.onProductSerializedImageChanged(base64)
                                viewModel.onProductImageUrlChanged("")
                            }
                        )
                    },
                    onFailure = { error -> Log.e("AddStockView", "Error: ${error.message}") }
                )
            }
        }
    }

    fun createCameraUri(): Uri? {
        return try {
            val photoFile = File(context.cacheDir, "product_photo_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile).also {
                cameraImageUri = it
            }
        } catch (e: Exception) { null }
    }

    val productForDisplay = remember(productName, productBrand, productCategory, productImageUrl, productSerializedImage, productData) {
        if (productName.isNotEmpty() || productBrand.isNotEmpty()) {
            com.lojasocial.app.domain.product.Product(
                name = productName,
                brand = productBrand,
                category = productCategory,
                imageUrl = productImageUrl,
                serializedImage = productSerializedImage
            )
        } else {
            (productData ?: com.lojasocial.app.domain.product.Product()).copy(
                serializedImage = productSerializedImage ?: productData?.serializedImage
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Adicionar Stock",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    ProductImage(
                        product = productForDisplay,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        contentDescription = "Imagem do Produto"
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { createCameraUri()?.let { cameraLauncher.launch(it) } },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = LojaSocialPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, "Foto", tint = Color.White)
                            }
                        }

                        Surface(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = LojaSocialPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoLibrary, "Galeria", tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isEditable = isManualMode || isBarcodeEditable

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            viewModel.onBarcodeChanged(it)
                            if (isBarcodeEditable && !isManualMode) {
                                viewModel.onProductNameChanged("")
                                viewModel.onProductBrandChanged("")
                                viewModel.onProductImageUrlChanged("")
                                viewModel.onProductSerializedImageChanged(null)
                            }
                        },
                        label = { Text("Código de Barras") },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading,
                        readOnly = !isEditable,
                        singleLine = true,
                        trailingIcon = if (!isEditable) {
                            {
                                IconButton(onClick = { isBarcodeEditable = true }) {
                                    Icon(Icons.Default.Edit, "Editar", tint = LojaSocialPrimary)
                                }
                            }
                        } else null
                    )
                    Button(
                        onClick = { if (barcode.isNotEmpty()) viewModel.fetchProductDataForCurrentBarcode() },
                        enabled = !uiState.isLoading && barcode.isNotEmpty(),
                        shape = RoundedCornerShape(128.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(56.dp)
                            .width(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Search, "Procurar")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Detalhes do Produto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LojaSocialPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = productName,
                    onValueChange = { viewModel.onProductNameChanged(it) },
                    label = { Text("Nome do Produto") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = productBrand,
                    onValueChange = { viewModel.onProductBrandChanged(it) },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stock e Validade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LojaSocialPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { viewModel.onQuantityChanged(it) },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Data de Validade", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = expiryDateEnabled, onCheckedChange = { expiryDateEnabled = it }, enabled = !uiState.isLoading)
                }

                if (expiryDateEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { viewModel.onExpiryDateChanged(it) },
                        label = { Text("DD/MM/AAAA") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "Selecionar Data")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                    CustomDatePickerDialog(
                        showDialog = showDatePicker,
                        onDateSelected = { d, m, y ->
                            viewModel.onExpiryDateChanged(String.format("%02d/%02d/%04d", d, m, y))
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                        campaigns.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    viewModel.onCampaignSelected(c)
                                    campaignExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!expiryDateEnabled) viewModel.onExpiryDateChanged("Sem Validade")
                viewModel.addToStock()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !uiState.isLoading && quantity.isNotEmpty() && (quantity.toIntOrNull() ?: 0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Adicionar ao Stock", fontWeight = FontWeight.Bold)
            }
        }

        uiState.successMessage?.let {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))) {
                Text(text = it, modifier = Modifier.padding(16.dp), color = Color.White)
            }
        }

        uiState.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = ScanRed)) {
                Text(text = it, modifier = Modifier.padding(16.dp), color = Color.White)
            }
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
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        )
    }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(isFlashOn) { cameraControl?.enableTorch(isFlashOn) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            processImageProxy(imageProxy, barcodeScanner, onBarcodeDetected)
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                    cameraControl = camera.cameraControl
                    camera.cameraControl.enableTorch(isFlashOn)
                } catch (exc: Exception) { Log.e("CameraPreview", "Binding failed", exc) }
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
                for (barcode in barcodes) barcode.rawValue?.let { onBarcodeDetected(it) }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScanningOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)
        }
        Box(modifier = Modifier.size(280.dp, 160.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val cornerSize = 20.dp.toPx()
                val blueColor = ScanBlue

                listOf(
                    Offset(0f, 0f) to listOf(Offset(cornerSize, 0f), Offset(0f, cornerSize)),
                    Offset(size.width, 0f) to listOf(Offset(size.width - cornerSize, 0f), Offset(size.width, cornerSize)),
                    Offset(0f, size.height) to listOf(Offset(0f, size.height - cornerSize), Offset(cornerSize, size.height)),
                    Offset(size.width, size.height) to listOf(Offset(size.width - cornerSize, size.height), Offset(size.width, size.height - cornerSize))
                ).forEach { (start, ends) ->
                    ends.forEach { end ->
                        drawLine(color = blueColor, start = start, end = end, strokeWidth = strokeWidth)
                    }
                }
            }
        }
    }
}