package com.lojasocial.app.ui.urgentRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.request.RequestItem
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.ui.requestitems.components.ProductItemRow
import com.lojasocial.app.ui.requests.components.RequestDetailsDialog
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestView(
    onBackClick: () -> Unit = {},
    viewModel: UrgentRequestViewModel = hiltViewModel(),
    requestsRepository: RequestsRepository? = null,
    userRepository: UserRepository? = null,
    profilePictureRepository: ProfilePictureRepository? = null,
    productRepository: ProductRepository? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val productQuantities by viewModel.productQuantities.collectAsState()
    val beneficiaries by viewModel.beneficiaries.collectAsState()
    val selectedBeneficiary by viewModel.selectedBeneficiary.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()
    val showCreateUserDialog by viewModel.showCreateUserDialog.collectAsState()
    val createdRequestId by viewModel.createdRequestId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showBeneficiarySelection by remember { mutableStateOf(false) }
    var showCreateUserForm by remember { mutableStateOf(false) }
    var createdRequest by remember { mutableStateOf<Request?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    val isSubmitting = submissionState is SubmissionState.Loading
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Fetch request details when created
    LaunchedEffect(createdRequestId) {
        if (createdRequestId != null && requestsRepository != null) {
            val result = requestsRepository.getRequestById(createdRequestId!!)
            result.fold(
                onSuccess = { request ->
                    createdRequest = request
                    // Fetch user profile for the request
                    if (userRepository != null && request.userId.isNotEmpty()) {
                        try {
                            val profile = userRepository.getUserProfile(request.userId).firstOrNull()
                            userProfile = profile
                        } catch (e: Exception) {
                            // Handle error silently
                        }
                    }
                },
                onFailure = {
                    snackbarHostState.showSnackbar(
                        message = "Erro ao carregar detalhes do pedido",
                        duration = SnackbarDuration.Long
                    )
                }
            )
        }
    }

    LaunchedEffect(submissionState) {
        when (val state = submissionState) {
            is SubmissionState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Entrega urgente criada com sucesso!",
                    duration = SnackbarDuration.Short
                )
            }
            is SubmissionState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSubmissionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when {
                            showCreateUserForm -> "Criar Novo Utilizador"
                            showBeneficiarySelection -> "Selecionar Beneficiário"
                            else -> "Entrega Urgente"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showCreateUserForm -> showCreateUserForm = false
                            showBeneficiarySelection -> showBeneficiarySelection = false
                            else -> onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!showBeneficiarySelection && !showCreateUserForm) {
                UrgentRequestBottomBar(
                    totalItemsSelected = productQuantities.values.sum(),
                    selectedBeneficiary = selectedBeneficiary,
                    onSelectBeneficiaryClick = { showBeneficiarySelection = true },
                    onSubmitClick = { viewModel.submitUrgentRequest() },
                    isSubmitting = isSubmitting
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                showCreateUserForm -> {
                    CreateUserForm(
                        onBackClick = { showCreateUserForm = false },
                        onCreateUser = { name, email ->
                            viewModel.createUserAndSelect(name, email)
                            showCreateUserForm = false
                            showBeneficiarySelection = false
                        }
                    )
                }
                showBeneficiarySelection -> {
                    BeneficiarySelectionScreen(
                        beneficiaries = beneficiaries,
                        selectedBeneficiary = selectedBeneficiary,
                        onSelectBeneficiary = { beneficiary ->
                            viewModel.selectBeneficiary(beneficiary)
                            showBeneficiarySelection = false
                        },
                        onCreateUserClick = {
                            showCreateUserForm = true
                        },
                        onBackClick = { showBeneficiarySelection = false },
                        profilePictureRepository = profilePictureRepository
                    )
                }
                else -> {
                    ItemSelectionScreen(
                        uiState = uiState,
                        productQuantities = productQuantities,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onAddProduct = { viewModel.onAddProduct(it) },
                        onRemoveProduct = { viewModel.onRemoveProduct(it) },
                        onLoadMore = { viewModel.fetchProducts(isLoadMore = true) },
                        isSubmitting = isSubmitting
                    )
                }
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LojaSocialPrimary)
                }
            }
        }

        // Show request details dialog when request is created
        createdRequest?.let { request ->
            RequestDetailsDialog(
                request = request,
                userName = userProfile?.name ?: "",
                userEmail = userProfile?.email ?: "",
                isLoading = false,
                onDismiss = {
                    createdRequest = null
                    userProfile = null
                    viewModel.resetSubmissionState()
                    onBackClick()
                },
                profilePictureRepository = profilePictureRepository,
                productRepository = productRepository,
                canAcceptReject = false, // No actions needed for completed urgent requests
                isBeneficiaryView = false
            )
        }
    }
}

@Composable
fun ItemSelectionScreen(
    uiState: UrgentRequestUiState,
    productQuantities: Map<String, Int>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddProduct: (String) -> Unit,
    onRemoveProduct: (String) -> Unit,
    onLoadMore: () -> Unit,
    isSubmitting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Pesquisar produtos...") },
            singleLine = true
        )

        when (uiState) {
            is UrgentRequestUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LojaSocialPrimary)
                }
            }
            is UrgentRequestUiState.Success -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val filteredProducts = uiState.products.filter { product ->
                        searchQuery.isBlank() || product.name.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    }

                    items(filteredProducts) { product ->
                        ProductItemRow(
                            product = product,
                            quantity = productQuantities[product.docId] ?: 0,
                            onAdd = {
                                if (!isSubmitting) onAddProduct(product.docId)
                            },
                            onRemove = {
                                if (!isSubmitting) onRemoveProduct(product.docId)
                            },
                            enabled = !isSubmitting
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = BorderColor
                        )
                    }
                }

                val isScrolledToEnd by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        if (layoutInfo.totalItemsCount == 0) {
                            false
                        } else {
                            val lastVisibleItem = visibleItemsInfo.last()
                            lastVisibleItem.index + 1 == layoutInfo.totalItemsCount
                        }
                    }
                }

                LaunchedEffect(isScrolledToEnd) {
                    if (isScrolledToEnd) {
                        onLoadMore()
                    }
                }
            }
            is UrgentRequestUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Erro ao carregar produtos", color = TextGray)
                }
            }
        }
    }
}

@Composable
fun BeneficiarySelectionScreen(
    beneficiaries: List<UserProfile>,
    selectedBeneficiary: UserProfile?,
    onSelectBeneficiary: (UserProfile) -> Unit,
    onCreateUserClick: () -> Unit,
    onBackClick: () -> Unit,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header removed - using top bar instead

        // Create new user button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onCreateUserClick),
            colors = CardDefaults.cardColors(containerColor = LojaSocialPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Criar Novo Utilizador",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Beneficiaries list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(beneficiaries) { beneficiary ->
                BeneficiaryItem(
                    beneficiary = beneficiary,
                    isSelected = beneficiary.uid == selectedBeneficiary?.uid,
                    onClick = { onSelectBeneficiary(beneficiary) },
                    profilePictureRepository = profilePictureRepository
                )
            }
        }
    }
}

@Composable
fun BeneficiaryItem(
    beneficiary: UserProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    var profilePictureBase64 by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Fetch profile picture
    LaunchedEffect(beneficiary.uid, profilePictureRepository) {
        if (profilePictureRepository != null) {
            try {
                profilePictureRepository.getProfilePicture(beneficiary.uid)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (Profile picture or initials)
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                        text = beneficiary.name.take(2).uppercase().ifEmpty { "U" },
                        color = Color(0xFF6B7280),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beneficiary.name.ifEmpty { "Sem nome" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = beneficiary.email,
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = LojaSocialPrimary
                )
            }
        }
    }
}

@Composable
fun CreateUserForm(
    onBackClick: () -> Unit,
    onCreateUser: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header removed - using top bar instead
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && email.isNotBlank()) {
                    onCreateUser(name, email)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = name.isNotBlank() && email.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
        ) {
            Text("Criar Utilizador", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UrgentRequestBottomBar(
    totalItemsSelected: Int,
    selectedBeneficiary: UserProfile?,
    onSelectBeneficiaryClick: () -> Unit,
    onSubmitClick: () -> Unit,
    isSubmitting: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Beneficiary selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectBeneficiaryClick),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedBeneficiary != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedBeneficiary != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedBeneficiary != null) {
                                selectedBeneficiary.name.ifEmpty { "Sem nome" }
                            } else {
                                "Selecionar Beneficiário"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedBeneficiary != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                        if (selectedBeneficiary != null) {
                            Text(
                                text = selectedBeneficiary.email,
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            // Submit button
            Button(
                onClick = onSubmitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = totalItemsSelected > 0 && selectedBeneficiary != null && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Criar Entrega Urgente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Items selecionados: $totalItemsSelected",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
