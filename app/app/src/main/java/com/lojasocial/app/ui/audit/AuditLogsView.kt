package com.lojasocial.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.audit.AuditLogEntry
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * View for displaying audit logs with date range selection.
 * 
 * This screen allows employees to:
 * - Select start and end dates for filtering logs
 * - View audit logs in a scrollable list
 * - See action type, timestamp, user ID, and details for each log entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsView(
    onNavigateBack: () -> Unit,
    viewModel: AuditLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val showStartDatePicker by viewModel.showStartDatePicker.collectAsState()
    val showEndDatePicker by viewModel.showEndDatePicker.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Registos de Auditoria",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Date selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Período",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Start date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data de início:",
                            fontSize = 14.sp,
                            color = TextGray,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.showStartDatePicker() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = startDate?.let { viewModel.formatDateForDisplay(it) } ?: "Selecionar",
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // End date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data de fim:",
                            fontSize = 14.sp,
                            color = TextGray,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.showEndDatePicker() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = endDate?.let { viewModel.formatDateForDisplay(it) } ?: "Selecionar",
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Fetch button
                    Button(
                        onClick = { viewModel.fetchLogs() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && startDate != null && endDate != null
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Carregar Registos")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFC62828),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Logs list
            if (uiState.logs.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (startDate == null || endDate == null) {
                            "Selecione um período para ver os registos"
                        } else {
                            "Nenhum registo encontrado para o período selecionado"
                        },
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.logs) { log ->
                        AuditLogCard(log, viewModel)
                    }
                }
            }
        }
    }

    // Date pickers
    CustomDatePickerDialog(
        showDialog = showStartDatePicker,
        onDateSelected = { day, month, year ->
            viewModel.onStartDateSelected(day, month, year)
        },
        onDismiss = { viewModel.dismissStartDatePicker() }
    )

    CustomDatePickerDialog(
        showDialog = showEndDatePicker,
        onDateSelected = { day, month, year ->
            viewModel.onEndDateSelected(day, month, year)
        },
        onDismiss = { viewModel.dismissEndDatePicker() }
    )
}

@Composable
fun AuditLogCard(
    log: AuditLogEntry,
    viewModel: AuditLogsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Action type
            Text(
                text = formatActionName(log.action, log.details),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data: ",
                    fontSize = 12.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = viewModel.formatTimestampForDisplay(log.timestamp),
                    fontSize = 12.sp,
                    color = TextDark
                )
            }
            
            if (log.userId != null || log.userName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // User Name
                    if (log.userName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Utilizador: ",
                                fontSize = 12.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = log.userName,
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                    }
                    // User ID
                    if (log.userId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ",
                                fontSize = 12.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = log.userId,
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }
            
            // Details
            log.details?.let { details ->
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detalhes:",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    details.forEach { (key, value) ->
                        Text(
                            text = "  • ${formatDetailKey(key)}: ${formatDetailValue(value)}",
                            fontSize = 12.sp,
                            color = TextDark,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatActionName(action: String, details: Map<String, Any>? = null): String {
    return when (action) {
        "add_item" -> "Adicionar Item"
        "remove_item" -> "Remover Item"
        "accept_request" -> "Aceitar Pedido"
        "decline_request" -> "Rejeitar Pedido"
        "accept_application" -> "Aceitar Candidatura"
        "decline_application" -> "Rejeitar Candidatura"
        else -> action.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
    }
}

fun formatDetailKey(key: String): String {
    return when (key) {
        "campaignId" -> "ID da Campanha"
        "itemId" -> "ID do Item"
        "quantity" -> "Quantidade"
        "barcode" -> "Código de Barras"
        "barcode_number" -> "Código de Barras"
        "productName" -> "Nome do Produto"
        "userId" -> "ID do Utilizador"
        else -> key.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
    }
}

fun formatDetailValue(value: Any?): String {
    return when (value) {
        null -> "N/A"
        is Map<*, *> -> Gson().toJson(value)
        is List<*> -> value.joinToString(", ")
        else -> value.toString()
    }
}
