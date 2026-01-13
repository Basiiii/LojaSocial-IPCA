package com.lojasocial.app.ui.requests.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.flow.firstOrNull
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.utils.FileUtils
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.ui.components.ProductImage
import com.lojasocial.app.ui.theme.BgYellow
import com.lojasocial.app.ui.theme.TextYellow
import java.util.Calendar
import com.lojasocial.app.domain.request.RequestStatus
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsDialog(
    request: Request,
    userName: String = "",
    userEmail: String = "",
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onAccept: (Date) -> Unit = {},
    onReject: (String?) -> Unit = {},
    onProposeNewDate: (Date) -> Unit = {},
    onComplete: () -> Unit = {},
    onCancelDelivery: (Boolean) -> Unit = {},
    profilePictureRepository: ProfilePictureRepository? = null,
    productRepository: ProductRepository? = null,
    canAcceptReject: Boolean = true,
    isBeneficiaryView: Boolean = false,
    onAcceptEmployeeDate: () -> Unit = {},
    onProposeNewDeliveryDate: (Date) -> Unit = {},
    currentUserId: String? = null,
    onRescheduleDelivery: (Date) -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showCancelDeliveryDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var beneficiaryAbsent by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Format date for display
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    val submissionDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    val formattedPickupDate = request.scheduledPickupDate?.let { dateFormat.format(it) }
    val formattedProposedDate = request.proposedDeliveryDate?.let { dateFormat.format(it) }

    // Determine status enum from status int
    val status = when (request.status) {
        0 -> RequestStatus.SUBMETIDO
        1 -> RequestStatus.PENDENTE_LEVANTAMENTO
        2 -> RequestStatus.CONCLUIDO
        3 -> RequestStatus.CANCELADO
        4 -> RequestStatus.REJEITADO
        else -> RequestStatus.SUBMETIDO
    }

    // Format products list with icons - use ProductCategory enum, sorted by category
    val productItems = request.items
        .sortedBy { item ->
            // Sort by category id: 1=Alimentar, 2=Casa, 3=Higiene, null/unknown=999 (last)
            item.category ?: 999
        }
        .map { item ->
            // Convert category Int to category string using ProductCategory enum
            val categoryString = when (ProductCategory.fromId(item.category)) {
                ProductCategory.ALIMENTAR -> "Alimentar"
                ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                ProductCategory.CASA -> "Limpeza"
                null -> "Vários"
            }
            ProductItemData(
                name = item.productName,
                quantity = item.quantity,
                brand = item.brand,
                expiryDate = item.expiryDate,
                icon = getCategoryIcon(categoryString),
                iconBgColor = getCategoryBgColor(categoryString),
                iconTint = getCategoryTintColor(categoryString),
                productDocId = item.productDocId
            )
        }

    // Determine request category using ProductCategory enum
    val requestCategory = when {
        request.items.isEmpty() -> "Vários"
        request.items.size > 1 -> "Vários"
        else -> {
            val firstItemCategory = request.items.firstOrNull()?.category ?: 1
            when (ProductCategory.fromId(firstItemCategory)) {
                ProductCategory.ALIMENTAR -> "Alimentar"
                ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                ProductCategory.CASA -> "Limpeza"
                null -> "Vários"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false // Essential for edge-to-edge
        )
    ) {
        val view = LocalView.current
        val themeColor = MaterialTheme.colorScheme.background.toArgb()

        SideEffect {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                ?: (view.context as? android.app.Activity)?.window

            if (window != null) {
                window.statusBarColor = themeColor
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                containerColor = Color(0xFFF8F9FA),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                when (status) {
                                    RequestStatus.SUBMETIDO -> "Pedido Pendente"
                                    RequestStatus.PENDENTE_LEVANTAMENTO -> "Pedido Aceite"
                                    RequestStatus.CONCLUIDO -> "Pedido Concluído"
                                    RequestStatus.REJEITADO -> "Pedido Rejeitado"
                                    RequestStatus.CANCELADO -> "Pedido Cancelado"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.White // This matches the status bar color we set
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. User Info Card
                        UserHeaderCard(
                            userId = request.userId,
                            userName = userName,
                            category = requestCategory,
                            status = status,
                            profilePictureRepository = profilePictureRepository
                        )

                        // 2. Product List Card
                        ProductListCard(
                            productItems = productItems,
                            productRepository = productRepository
                        )

                        // 3. Proposed Delivery Date Card (from beneficiary) - only show if employee hasn't proposed a date
                        if (status == RequestStatus.SUBMETIDO && formattedProposedDate != null && request.scheduledPickupDate == null) {
                            ProposedDeliveryDateCard(
                                proposedDate = formattedProposedDate
                            )
                        }

                        // 4. Employee Proposed Date Card (shown to beneficiaries when employee has proposed a date)
                        if (status == RequestStatus.SUBMETIDO && request.scheduledPickupDate != null) {
                            if (isBeneficiaryView) {
                                EmployeeProposedDateCard(
                                    proposedDate = formattedPickupDate ?: ""
                                )
                            } else {
                                SuggestedDateCard(
                                    suggestedDate = formattedPickupDate ?: ""
                                )
                            }
                        }

                        // 5. Collection Info Card
                        if (status == RequestStatus.REJEITADO) {
                            // Show rejection reason for rejected requests
                            CollectionInfoCard(
                                pickupDate = "",
                                rejectionReason = request.rejectionReason
                            )
                        } else if (status != RequestStatus.SUBMETIDO) {
                            // Show pickup date for other non-submitted requests
                            CollectionInfoCard(
                                pickupDate = formattedPickupDate ?: "",
                                rejectionReason = null
                            )
                        }
                    }

                    // Fixed Footer Buttons
                    // Logic: Show buttons only to the party that needs to respond
                    // - If scheduledPickupDate exists → Employee has proposed, beneficiary needs to respond
                    // - If scheduledPickupDate is null and proposedDeliveryDate exists → Beneficiary has proposed, employee needs to respond
                    // - If current user owns the request and viewing from employee side → Show only info, no buttons
                    if (status == RequestStatus.SUBMETIDO && canAcceptReject) {
                        val isOwnRequest = currentUserId != null && currentUserId == request.userId
                        val employeeHasProposed = request.scheduledPickupDate != null
                        
                        // If user owns the request and is viewing from employee side, show only info
                        if (isOwnRequest && !isBeneficiaryView) {
                            // No buttons shown - user viewing their own request from employee side
                        } else if (employeeHasProposed) {
                            // Employee has proposed a date (counter-proposal) - only beneficiary can respond
                            if (isBeneficiaryView) {
                                BeneficiaryActionFooter(
                                    isLoading = isLoading,
                                    onAcceptClick = {
                                        onAcceptEmployeeDate()
                                    },
                                    onProposeDateClick = { 
                                        // Show date picker to propose a new date (counter-proposal)
                                        showDatePicker = true
                                    },
                                    onRejectClick = { showRejectDialog = true }
                                )
                            }
                            // Employee side: no buttons shown (waiting for beneficiary response)
                        } else {
                            // Beneficiary has proposed (initial or counter-proposal) - only employee can respond
                            if (!isBeneficiaryView) {
                                ActionFooter(
                                    isLoading = isLoading,
                                    isAcceptEnabled = request.proposedDeliveryDate != null,
                                    onAcceptClick = {
                                        // Accept beneficiary's proposed date
                                        request.proposedDeliveryDate?.let { date ->
                                            onAccept(date)
                                        }
                                    },
                                    onProposeDateClick = { 
                                        // Show date picker to propose a new date (counter-proposal)
                                        showDatePicker = true
                                    },
                                    onRejectClick = { showRejectDialog = true }
                                )
                            }
                            // Beneficiary side: no buttons shown (waiting for employee response)
                        }
                    } else if (status == RequestStatus.PENDENTE_LEVANTAMENTO && canAcceptReject) {
                        // For employees: Show Complete, Cancel, and Reschedule buttons
                        // For beneficiaries: Show Reschedule button
                        // If user owns the request and is viewing from employee side, show only info
                        val isOwnRequest = currentUserId != null && currentUserId == request.userId
                        if (!isBeneficiaryView && !isOwnRequest) {
                            // Employee side
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Complete Button (Green)
                                Button(
                                    onClick = {
                                        onComplete()
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF156946)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Concluir Pedido", fontSize = 16.sp, color = Color.White)
                                    }
                                }
                                
                                // Reschedule Button (Blue - same as "Propor data")
                                Button(
                                    onClick = {
                                        showDatePicker = true
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D75F0)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reagendar Entrega", fontSize = 16.sp, color = Color.White)
                                }
                                
                                // Cancel Delivery Button (Red)
                                Button(
                                    onClick = {
                                        showCancelDeliveryDialog = true
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancelar Entrega", fontSize = 16.sp, color = Color.White)
                                }
                            }
                        } else if (isBeneficiaryView) {
                            // Beneficiary side - show Reschedule button
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showDatePicker = true
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D75F0)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reagendar Entrega", fontSize = 16.sp, color = Color.White)
                                }
                            }
                        }
                        // If user owns the request and is viewing from employee side, no buttons are shown
                    } else {
                        // Close Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF156946)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Fechar", fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // ... (Keep your DatePicker and RejectDialog code here exactly as before) ...
    // Date Picker Dialog
    CustomDatePickerDialog(
        showDialog = showDatePicker,
        onDateSelected = { day, month, year ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDate = calendar.time
            showDatePicker = false
            // If status is PENDENTE_LEVANTAMENTO, use reschedule; otherwise use normal propose flow
            val currentStatus = when (request.status) {
                0 -> RequestStatus.SUBMETIDO
                1 -> RequestStatus.PENDENTE_LEVANTAMENTO
                2 -> RequestStatus.CONCLUIDO
                3 -> RequestStatus.CANCELADO
                4 -> RequestStatus.REJEITADO
                else -> RequestStatus.SUBMETIDO
            }
            if (currentStatus == RequestStatus.PENDENTE_LEVANTAMENTO) {
                // Reschedule delivery
                onRescheduleDelivery(calendar.time)
            } else {
                // Normal propose flow
                if (isBeneficiaryView) {
                    onProposeNewDeliveryDate(calendar.time)
                } else {
                    onProposeNewDate(calendar.time)
                }
            }
        },
        onDismiss = { showDatePicker = false },
        initialYear = selectedDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.YEAR)
        },
        initialMonth = selectedDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.MONTH) + 1
        },
        initialDay = selectedDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.DAY_OF_MONTH)
        },
        minDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )

    // Reject Dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rejeitar Pedido") },
            text = {
                Column {
                    Text("Tem a certeza que deseja rejeitar este pedido?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        onReject(if (rejectReason.isBlank()) null else rejectReason)
                        rejectReason = ""
                    }
                ) {
                    Text("Rejeitar", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Cancel Delivery Dialog
    if (showCancelDeliveryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCancelDeliveryDialog = false
                beneficiaryAbsent = false
            },
            title = { Text("Cancelar Entrega") },
            text = {
                Column {
                    Text("Tem a certeza que deseja cancelar esta entrega?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = beneficiaryAbsent,
                            onCheckedChange = { beneficiaryAbsent = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "O beneficiário faltou?",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDeliveryDialog = false
                        onCancelDelivery(beneficiaryAbsent)
                        beneficiaryAbsent = false
                    }
                ) {
                    Text("Confirmar", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCancelDeliveryDialog = false
                    beneficiaryAbsent = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Data class for product items
data class ProductItemData(
    val name: String,
    val quantity: Int,
    val brand: String = "",
    val expiryDate: Date? = null,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color,
    val productDocId: String = "" // For fetching product image
)

@Composable
fun UserHeaderCard(
    userId: String,
    userName: String,
    category: String,
    status: RequestStatus,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    var profilePictureBase64 by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Fetch profile picture
    LaunchedEffect(userId, profilePictureRepository) {
        if (profilePictureRepository != null) {
            try {
                profilePictureRepository.getProfilePicture(userId)
                    .firstOrNull()
                    ?.let { base64 ->
                        profilePictureBase64 = base64
                        // Decode Base64 to ImageBitmap
                        if (!base64.isNullOrBlank()) {
                            val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                            bytes?.let {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                imageBitmap = bitmap?.asImageBitmap()
                            }
                        }
                    }
            } catch (e: Exception) {
                // Handle error silently, fallback to initials
            }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (Profile picture or initials)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = userName.take(2).uppercase(),
                        color = Color(0xFF6B7280),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "Utilizador" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = category,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Status Badge
            Surface(
                color = status.iconBgColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status.label,
                    color = status.iconTint,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProductListCard(
    productItems: List<ProductItemData>,
    productRepository: ProductRepository? = null
) {
    // Date format for displaying expiry dates
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (productItems.isEmpty()) {
                Text(
                    text = "Nenhum produto",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            } else {
                productItems.forEach { item ->
                    ProductItemRow(
                        item = item,
                        dateFormat = dateFormat,
                        productRepository = productRepository,
                        firestore = firestore
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductItemRow(
    item: ProductItemData,
    dateFormat: SimpleDateFormat,
    productRepository: ProductRepository?,
    firestore: FirebaseFirestore
) {
    var product by remember(item.productDocId) { mutableStateOf<Product?>(null) }
    var hasImage by remember { mutableStateOf(false) }
    
    // Fetch product to get image
    LaunchedEffect(item.productDocId, productRepository) {
        if (item.productDocId.isNotEmpty() && productRepository != null) {
            try {
                // First get the item document to extract barcode
                val itemDoc = withContext(Dispatchers.IO) {
                    firestore.collection("items").document(item.productDocId).get().await()
                }
                
                if (itemDoc.exists()) {
                    val itemData = itemDoc.data
                    val barcode = (itemData?.get("barcode") as? String)
                        ?: (itemData?.get("productId") as? String)
                        ?: itemData?.get("productId")?.toString()
                    
                    if (barcode != null) {
                        // Fetch product using barcode
                        val fetchedProduct = withContext(Dispatchers.IO) {
                            productRepository.getProductByBarcodeId(barcode)
                        }
                        product = fetchedProduct
                        hasImage = fetchedProduct?.let { 
                            !it.imageUrl.isNullOrEmpty() || !it.serializedImage.isNullOrBlank() 
                        } ?: false
                    }
                }
            } catch (e: Exception) {
                // Handle error silently, will fallback to icon
                product = null
                hasImage = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image or Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (hasImage) Color.Transparent else item.iconBgColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && product != null) {
                    ProductImage(
                        product = product,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        backgroundColor = Color.White
                    )
                } else {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and Brand Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                if (item.brand.isNotEmpty()) {
                    Text(
                        text = item.brand,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Quantity
            Text(
                text = "Qtd: ${item.quantity}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        
        // Expiry Date Row
        if (item.expiryDate != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Validade: ${dateFormat.format(item.expiryDate)}",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626)
                )
            }
        }
    }
}

@Composable
fun ProposedDeliveryDateCard(
    proposedDate: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Data de Entrega Proposta",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Calendar Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BgYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Proposta pelo beneficiário",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = proposedDate,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeProposedDateCard(
    proposedDate: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Data Proposta pelo Funcionário",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Calendar Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD1E4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF2D75F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Proposta pelo funcionário",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = proposedDate,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedDateCard(
    suggestedDate: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nova Data Proposta",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Calendar Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD1E4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF2D75F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Data proposta pelo funcionário",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = suggestedDate,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionInfoCard(
    pickupDate: String,
    rejectionReason: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (rejectionReason != null) {
                Text(
                    text = "Motivo da rejeição",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = rejectionReason.ifEmpty { "Sem motivo especificado." },
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "Data de recolha",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Blue Arrow Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFD1E4FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF2D75F0),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Recolha",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (pickupDate.isNotEmpty()) {
                            Text(
                                text = pickupDate,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "Data a definir",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Campus do IPCA, Barcelos",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionFooter(
    isLoading: Boolean,
    isAcceptEnabled: Boolean = true,
    onAcceptClick: () -> Unit,
    onProposeDateClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accept Button (Green)
        Button(
            onClick = onAcceptClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && isAcceptEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF156946)), // Dark Green
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aceitar Recolha", fontSize = 16.sp)
            }
        }

        // Secondary Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Propose Date (Blue)
            Button(
                onClick = onProposeDateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D75F0)), // Blue
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Propor data", fontSize = 16.sp)
            }

            // Reject (Red)
            Button(
                onClick = onRejectClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rejeitar", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun BeneficiaryActionFooter(
    isLoading: Boolean,
    onAcceptClick: () -> Unit,
    onProposeDateClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accept Button (Green)
        Button(
            onClick = onAcceptClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF156946)), // Dark Green
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aceitar Data", fontSize = 16.sp)
            }
        }

        // Secondary Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Propose Date (Blue)
            Button(
                onClick = onProposeDateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D75F0)), // Blue
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Propor data", fontSize = 16.sp)
            }

            // Reject (Red)
            Button(
                onClick = onRejectClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rejeitar", fontSize = 16.sp)
            }
        }
    }
}

// Helper function to determine category from product name
fun getProductCategoryFromName(productName: String): String {
    val nameLower = productName.lowercase()
    return when {
        nameLower.contains("limpeza") || nameLower.contains("detergente") || nameLower.contains("sabão") -> "Limpeza"
        nameLower.contains("comida") || nameLower.contains("alimento") || nameLower.contains("arroz") ||
        nameLower.contains("massa") || nameLower.contains("pão") || nameLower.contains("leite") -> "Alimentar"
        nameLower.contains("higiene") || nameLower.contains("sabonete") || nameLower.contains("desodorizante") ||
        nameLower.contains("shampoo") || nameLower.contains("pasta") && nameLower.contains("dente") -> "Higiene"
        else -> "Vários"
    }
}

// Helper function to get category icon
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "limpeza" -> Icons.Default.CleaningServices
        "alimentar" -> Icons.Default.Restaurant
        "higiene" -> Icons.Default.Spa
        else -> Icons.Default.Work // Default for "Vários"
    }
}

// Helper function to get category background color
fun getCategoryBgColor(category: String): Color {
    return when (category.lowercase()) {
        "limpeza" -> Color(0xFFD1E4FF) // Light Blue
        "alimentar" -> BgYellow
        "higiene" -> Color(0xFFDFF7E2) // Light Green
        else -> Color(0xFFE5E7EB) // Light Gray
    }
}

// Helper function to get category tint color
fun getCategoryTintColor(category: String): Color {
    return when (category.lowercase()) {
        "limpeza" -> Color(0xFF2D75F0) // Blue
        "alimentar" -> TextYellow
        "higiene" -> Color(0xFF1B5E20) // Green
        else -> Color(0xFF6B7280) // Gray
    }
}
