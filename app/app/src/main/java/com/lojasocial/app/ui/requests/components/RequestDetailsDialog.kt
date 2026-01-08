package com.lojasocial.app.ui.requests.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import com.lojasocial.app.domain.request.RequestStatus
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.ui.components.CustomDatePickerDialog
import com.lojasocial.app.ui.theme.BodyText
import com.lojasocial.app.ui.theme.ButtonGreen
import com.lojasocial.app.ui.theme.HeaderText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RequestDetailsDialog(
    request: Request,
    userName: String = "",
    userEmail: String = "",
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onAccept: (Date) -> Unit = {},
    onReject: (String?) -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    // Format date for display
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")) }
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

    // Format products list
    val productsList = request.items.map { item ->
        "${item.quantity}x ${item.productName}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Header Section (Icon + Status Title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(status.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = status.icon,
                            contentDescription = null,
                            tint = status.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = status.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderText
                    )
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                // 2. User Information Section
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Informações do utilizador",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (userName.isNotEmpty()) {
                        Text(
                            text = "Nome: $userName",
                            fontSize = 15.sp,
                            color = BodyText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (userEmail.isNotEmpty()) {
                        Text(
                            text = "Email: $userEmail",
                            fontSize = 15.sp,
                            color = BodyText
                        )
                    }
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                // 3. Products List Section
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Produtos no cabaz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (productsList.isEmpty()) {
                        Text(
                            text = "Nenhum produto",
                            fontSize = 15.sp,
                            color = BodyText,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        productsList.forEach { product ->
                            Text(
                                text = product,
                                fontSize = 15.sp,
                                color = BodyText,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                // 4. Dynamic Section (Date OR Rejection Reason)
                if (status != RequestStatus.SUBMETIDO) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        if (status == RequestStatus.REJEITADO) {
                            Text(
                                text = "Motivo da rejeição",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = request.rejectionReason ?: "Sem motivo especificado.",
                                fontSize = 15.sp,
                                color = BodyText,
                                lineHeight = 22.sp
                            )
                        } else {
                            val label = if (status == RequestStatus.CONCLUIDO)
                                "Levantado em" else "Data de levantamento"

                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formattedPickupDate ?: "Data a definir",
                                fontSize = 15.sp,
                                color = BodyText
                            )
                        }
                    }
                    Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                }

                // 5. Action Buttons (only for SUBMETIDO status)
                if (status == RequestStatus.SUBMETIDO) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Accept Button
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Aceitar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        // Reject Button
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626) // Red
                            )
                        ) {
                            Text(
                                text = "Rejeitar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Close Button for other statuses
                    Box(modifier = Modifier.padding(24.dp)) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Fechar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog (using CustomDatePickerDialog)
    CustomDatePickerDialog(
        showDialog = showDatePicker,
        onDateSelected = { day, month, year ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1) // month is 1-based in callback, convert to 0-based
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDate = calendar.time
            selectedDate?.let { onAccept(it) }
            showDatePicker = false
        },
        onDismiss = { showDatePicker = false },
        initialYear = selectedDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.YEAR)
        },
        initialMonth = selectedDate?.let {
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.MONTH) + 1 // Convert to 1-based
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

    // Reject Confirmation Dialog
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
