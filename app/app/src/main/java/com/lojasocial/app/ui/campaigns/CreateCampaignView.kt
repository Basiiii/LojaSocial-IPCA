package com.lojasocial.app.ui.campaigns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.data.model.Campaign
import com.lojasocial.app.repository.CampaignRepository
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.lojasocial.app.ui.theme.ButtonGreen
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import androidx.compose.runtime.rememberCoroutineScope
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.RedDelete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(
    campaignRepository: CampaignRepository?,
    campaignId: String? = null,
    campaignToEdit: Campaign? = null,
    onNavigateBack: () -> Unit,
    onCampaignSaved: () -> Unit
) {
    var campaignName by remember { mutableStateOf("") }
    var selectedStartDate by remember { mutableStateOf<Date?>(null) }
    var selectedEndDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var campaign by remember { mutableStateOf<Campaign?>(campaignToEdit) }
    var shouldSave by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
    
    // Load campaign if editing
    LaunchedEffect(campaignId) {
        if (campaignId != null && campaign == null && campaignRepository != null) {
            try {
                val allCampaigns = campaignRepository.getAllCampaigns()
                campaign = allCampaigns.find { it.id == campaignId }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Initialize form with campaign data
    LaunchedEffect(campaign) {
        campaign?.let {
            campaignName = it.name
            selectedStartDate = it.startDate
            selectedEndDate = it.endDate
        }
    }
    
    val startDateString = selectedStartDate?.let { dateFormatter.format(it) } ?: ""
    val endDateString = selectedEndDate?.let { dateFormatter.format(it) } ?: ""
    
    fun handleSave() {
        if (campaignName.isBlank() || selectedStartDate == null || selectedEndDate == null) {
            return
        }
        
        if (campaignRepository == null) {
            android.util.Log.e("CreateCampaign", "CampaignRepository is null")
            return
        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                val campaignToSave = campaign?.copy(
                    id = campaign!!.id,
                    name = campaignName,
                    startDate = selectedStartDate!!,
                    endDate = selectedEndDate!!
                ) ?: Campaign(
                    name = campaignName,
                    startDate = selectedStartDate!!,
                    endDate = selectedEndDate!!
                )
                
                // Use withTimeout to limit the operation to 10 seconds
                val result = withTimeout(10000) { // 10 seconds timeout
                    if (campaign != null) {
                        // Update existing campaign
                        campaignRepository!!.updateCampaign(campaignToSave)
                    } else {
                        // Create new campaign - convert Result<String> to Result<Unit>
                        campaignRepository!!.createCampaign(campaignToSave).map { Unit }
                    }
                }
                
                result.fold(
                    onSuccess = {
                        android.util.Log.d("CreateCampaign", "Campaign saved successfully")
                        val message = if (campaign != null) {
                            "Campanha atualizada com sucesso"
                        } else {
                            "Campanha criada com sucesso"
                        }
                        // Show snackbar first
                        snackbarHostState.showSnackbar(message)
                        // Navigate back after showing the snackbar
                        delay(1500) // Give user time to see the message
                        onCampaignSaved()
                    },
                    onFailure = { error ->
                        android.util.Log.e("CreateCampaign", "Error saving campaign: ${error.message}", error)
                        val errorMessage = "Erro ao guardar campanha: ${error.message ?: "Erro desconhecido"}"
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                )
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("CreateCampaign", "Timeout saving campaign: ${e.message}", e)
                snackbarHostState.showSnackbar("Erro: Não foi possível adicionar a campanha. Por favor, tente novamente.")
            } catch (e: Exception) {
                android.util.Log.e("CreateCampaign", "Exception saving campaign: ${e.message}", e)
                val errorMessage = "Erro ao guardar campanha: ${e.message ?: "Erro desconhecido"}"
                snackbarHostState.showSnackbar(errorMessage)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
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
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (campaign != null) "Editar Campanha" else "Criar Campanha",
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
        bottomBar = {
            // Fixed Bottom Button
            ContainerWithShadow {
                Button(
                    onClick = { handleSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading && campaignName.isNotBlank() && selectedStartDate != null && selectedEndDate != null,
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (campaign != null) "Guardar Alterações" else "Criar Campanha",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Gray Header Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Detalhes da Campanha",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }

            // Form Content
            Column(modifier = Modifier.padding(24.dp)) {
                // 1. Campaign Name
                InputLabel("Nome da Campanha")
                OutlinedTextField(
                    value = campaignName,
                    onValueChange = { campaignName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Insira o nome da campanha", color = Color(0xFF9CA3AF)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedBorderColor = ButtonGreen,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Start Date
                InputLabel("Data de Início")
                ReadOnlyDateInput(
                    value = startDateString,
                    placeholder = "dd/mm/aaaa",
                    onClick = { showStartDatePicker = true },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. End Date
                InputLabel("Data de Fim")
                ReadOnlyDateInput(
                    value = endDateString,
                    placeholder = "dd/mm/aaaa",
                    onClick = { showEndDatePicker = true },
                    enabled = !isLoading
                )
            }
        }
    }
    
    // Start Date Picker
    CustomDatePickerDialog(
        showDialog = showStartDatePicker,
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
            selectedStartDate = calendar.time
            showStartDatePicker = false
        },
        onDismiss = { showStartDatePicker = false },
        initialYear = selectedStartDate?.let { 
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.YEAR)
        },
        initialMonth = selectedStartDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.MONTH) + 1
        },
        initialDay = selectedStartDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.DAY_OF_MONTH)
        },
        maxDate = selectedEndDate?.time
    )
    
    // End Date Picker
    CustomDatePickerDialog(
        showDialog = showEndDatePicker,
        onDateSelected = { day, month, year ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            selectedEndDate = calendar.time
            showEndDatePicker = false
        },
        onDismiss = { showEndDatePicker = false },
        initialYear = selectedEndDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.YEAR)
        },
        initialMonth = selectedEndDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.MONTH) + 1
        },
        initialDay = selectedEndDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.DAY_OF_MONTH)
        },
        minDate = selectedStartDate?.time
    )
}

// --- Helper Components ---

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF374151),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ReadOnlyDateInput(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            trailingIcon = {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = Color(0xFF9CA3AF))
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = TextDark,
                disabledBorderColor = Color(0xFFE5E7EB),
                disabledPlaceholderColor = Color(0xFF9CA3AF),
                disabledContainerColor = Color.White,
                disabledTrailingIconColor = Color(0xFF9CA3AF)
            )
        )

        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            )
        }
    }
}

@Composable
fun ContainerWithShadow(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}


