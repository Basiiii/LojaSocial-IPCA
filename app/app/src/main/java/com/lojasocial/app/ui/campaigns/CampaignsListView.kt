package com.lojasocial.app.ui.campaigns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.ui.campaigns.components.CampaignCard
import com.lojasocial.app.ui.components.StatusTabSelector
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.RedDelete
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import java.util.*

enum class CampaignTab {
    ACTIVE,    // A decorrer
    FINISHED,  // Terminadas
    UPCOMING   // Em breve
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignsListView(
    campaignRepository: CampaignRepository,
    onNavigateBack: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (Campaign) -> Unit = {},
    onCampaignClick: (Campaign) -> Unit = {}
) {
    var campaigns by remember { mutableStateOf<List<Campaign>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreCampaigns by remember { mutableStateOf(true) }
    var selectedTabString by remember { mutableStateOf("A decorrer") }
    var showDeleteDialog by remember { mutableStateOf<Campaign?>(null) }
    var campaignToDelete by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Track last loaded date for pagination
    var lastLoadedStartDate by remember { mutableStateOf<Date?>(null) }
    
    // Convert string to enum for filtering logic
    val selectedTab = remember(selectedTabString) {
        when (selectedTabString) {
            "A decorrer" -> CampaignTab.ACTIVE
            "Terminadas" -> CampaignTab.FINISHED
            "Em breve" -> CampaignTab.UPCOMING
            else -> CampaignTab.ACTIVE
        }
    }
    
    val tabOptions = listOf("A decorrer", "Terminadas", "Em breve")

    // Load initial campaigns
    LaunchedEffect(Unit) {
        try {
            val (loadedCampaigns, hasMore) = campaignRepository.getCampaignsPaginated(limit = 15)
            campaigns = loadedCampaigns
            hasMoreCampaigns = hasMore
            lastLoadedStartDate = loadedCampaigns.lastOrNull()?.startDate
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }
    
    // Reset pagination when tab changes
    LaunchedEffect(selectedTabString) {
        // Reset to initial state when tab changes
        campaigns = emptyList()
        hasMoreCampaigns = true
        lastLoadedStartDate = null
        isLoading = true
        try {
            val (loadedCampaigns, hasMore) = campaignRepository.getCampaignsPaginated(limit = 15)
            campaigns = loadedCampaigns
            hasMoreCampaigns = hasMore
            lastLoadedStartDate = loadedCampaigns.lastOrNull()?.startDate
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }
    
    // Load more campaigns when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Pair(lastVisibleItemIndex, totalItems)
        }.collect { (lastVisibleIndex, totalItems) ->
            // Load more when user is within 3 items of the end
            if (lastVisibleIndex >= totalItems - 3 && 
                hasMoreCampaigns && 
                !isLoadingMore && 
                !isLoading) {
                isLoadingMore = true
                try {
                    val (loadedCampaigns, hasMore) = campaignRepository.getCampaignsPaginated(
                        limit = 15,
                        lastStartDate = lastLoadedStartDate
                    )
                    if (loadedCampaigns.isNotEmpty()) {
                        campaigns = campaigns + loadedCampaigns
                        hasMoreCampaigns = hasMore
                        lastLoadedStartDate = loadedCampaigns.lastOrNull()?.startDate
                    } else {
                        hasMoreCampaigns = false
                    }
                } catch (e: Exception) {
                    // Error loading more - stop trying
                    hasMoreCampaigns = false
                } finally {
                    isLoadingMore = false
                }
            }
        }
    }
    
    // Auto-load more if filtered list is too short (less than 5 items visible)
    LaunchedEffect(campaigns, selectedTabString) {
        val now = Date()
        val filtered = campaigns.filter { campaign ->
            when (selectedTab) {
                CampaignTab.ACTIVE -> campaign.startDate <= now && campaign.endDate >= now
                CampaignTab.FINISHED -> campaign.endDate < now
                CampaignTab.UPCOMING -> campaign.startDate > now
            }
        }
        
        // If filtered list is too short and we have more to load, load more
        if (filtered.size < 5 && hasMoreCampaigns && !isLoadingMore && !isLoading) {
            isLoadingMore = true
            try {
                val (loadedCampaigns, hasMore) = campaignRepository.getCampaignsPaginated(
                    limit = 15,
                    lastStartDate = lastLoadedStartDate
                )
                if (loadedCampaigns.isNotEmpty()) {
                    campaigns = campaigns + loadedCampaigns
                    hasMoreCampaigns = hasMore
                    lastLoadedStartDate = loadedCampaigns.lastOrNull()?.startDate
                } else {
                    hasMoreCampaigns = false
                }
            } catch (e: Exception) {
                hasMoreCampaigns = false
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    // Handle campaign deletion
    LaunchedEffect(campaignToDelete) {
        campaignToDelete?.let { campaignId ->
            val result = campaignRepository.deleteCampaign(campaignId)
            result.fold(
                onSuccess = {
                    // Successfully deleted - update local state
                    campaigns = campaigns.filter { it.id != campaignId }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Campanha eliminada com sucesso")
                    }
                },
                onFailure = { error ->
                    // Deletion failed - show error message
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Erro ao eliminar campanha: ${error.message ?: "Erro desconhecido"}"
                        )
                    }
                }
            )
            campaignToDelete = null
            showDeleteDialog = null
        }
    }

    val now = Date()
    
    // Filter campaigns based on selected tab
    val filteredCampaigns = remember(campaigns, selectedTab, now) {
        campaigns.filter { campaign ->
            when (selectedTab) {
                CampaignTab.ACTIVE -> {
                    campaign.startDate <= now && campaign.endDate >= now
                }
                CampaignTab.FINISHED -> {
                    campaign.endDate < now
                }
                CampaignTab.UPCOMING -> {
                    campaign.startDate > now
                }
            }
        }.sortedByDescending { it.startDate }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                val isSuccess = snackbarData.visuals.message.contains("sucesso", ignoreCase = true)
                val isError = snackbarData.visuals.message.contains("Erro", ignoreCase = true)
                
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = when {
                        isSuccess -> LojaSocialPrimary // Green for success
                        isError -> RedDelete // Red for error
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Campanhas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.DarkGray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar",
                            tint = Color.DarkGray
                        )
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
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            StatusTabSelector(
                options = tabOptions,
                selectedOption = selectedTabString,
                onOptionSelected = { selectedTabString = it }
            )

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredCampaigns.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    CampaignTab.ACTIVE -> "Nenhuma campanha a decorrer"
                                    CampaignTab.FINISHED -> "Nenhuma campanha terminada"
                                    CampaignTab.UPCOMING -> "Nenhuma campanha em breve"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCampaigns) { campaign ->
                            val isFinished = campaign.endDate < now
                            CampaignCard(
                                campaign = campaign,
                                isFinished = isFinished,
                                onClick = {
                                    onCampaignClick(campaign)
                                },
                                onEditClick = {
                                    onEditClick(campaign)
                                },
                                onDeleteClick = {
                                    showDeleteDialog = campaign
                                }
                            )
                        }
                        
                        // Show loading indicator at the bottom when loading more
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { campaign ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text("Eliminar Campanha")
            },
            text = {
                Text("Tem a certeza que quer eliminar a campanha \"${campaign.name}\"? Esta ação não pode ser revertida.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        campaignToDelete = campaign.id
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

