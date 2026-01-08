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
import com.lojasocial.app.ui.theme.BgYellow
import com.lojasocial.app.ui.theme.TextYellow
import java.util.Calendar
import com.lojasocial.app.domain.request.RequestStatus
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.lojasocial.app.ui.theme.LojaSocialBackground
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
    profilePictureRepository: ProfilePictureRepository? = null,
    canAcceptReject: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    val scrollState = rememberScrollState()

    // Format date for display
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    val submissionDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    val formattedPickupDate = request.scheduledPickupDate?.let { dateFormat.format(it) }

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
                iconTint = getCategoryTintColor(categoryString)
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
                                    fontSize = 18.sp
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
                        ProductListCard(productItems = productItems)

                        // 3. Suggested Date Card (only for SUBMETIDO status when date is selected)
                        if (status == RequestStatus.SUBMETIDO && selectedDate != null) {
                            SuggestedDateCard(
                                suggestedDate = dateFormat.format(selectedDate!!)
                            )
                        }

                        // 4. Collection Info Card
                        if (status != RequestStatus.SUBMETIDO && status != RequestStatus.REJEITADO) {
                            CollectionInfoCard(
                                pickupDate = formattedPickupDate ?: "",
                                rejectionReason = if (status == RequestStatus.REJEITADO) request.rejectionReason else null
                            )
                        }
                    }

                    // Fixed Footer Buttons
                    if (status == RequestStatus.SUBMETIDO && canAcceptReject) {
                        ActionFooter(
                            isLoading = isLoading,
                            isAcceptEnabled = selectedDate != null,
                            onAcceptClick = {
                                selectedDate?.let { date ->
                                    onAccept(date)
                                }
                            },
                            onProposeDateClick = { showDatePicker = true },
                            onRejectClick = { showRejectDialog = true }
                        )
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
}

// Data class for product items
data class ProductItemData(
    val name: String,
    val quantity: Int,
    val brand: String = "",
    val expiryDate: Date? = null,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color
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
fun ProductListCard(productItems: List<ProductItemData>) {
    // Date format for displaying expiry dates
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")) }
    
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
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(item.iconBgColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
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
                text = "Data Sugerida",
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
                        text = "Data proposta",
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
