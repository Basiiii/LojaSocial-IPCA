package com.lojasocial.app.ui.requests.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lojasocial.app.data.model.RequestStatus
import com.lojasocial.app.ui.theme.BodyText
import com.lojasocial.app.ui.theme.ButtonGreen
import com.lojasocial.app.ui.theme.HeaderText

// --- Data Model for the Dialog ---
data class RequestDetails(
    val status: RequestStatus,
    val products: List<String>,
    val pickupDate: String? = null,
    val pickupTimeRange: String? = null,
    val rejectionReason: String? = null
)

@Composable
fun RequestDetailsDialog(
    request: RequestDetails,
    onDismiss: () -> Unit
) {
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
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Header Section (Icon + Status Title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dynamic Icon Box
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(request.status.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = request.status.icon,
                            contentDescription = null,
                            tint = request.status.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Dynamic Title
                    // Using .label from the Enum as defined in the previous step
                    Text(
                        text = request.status.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderText
                    )
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                // 2. Products List Section
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Produtos no cabaz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Simple list of products
                    request.products.forEach { product ->
                        Text(
                            text = product,
                            fontSize = 15.sp,
                            color = BodyText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                // 3. Dynamic Section (Date OR Rejection Reason)
                // HIDE if status is SUBMETIDO (Yellow state)
                if (request.status != RequestStatus.SUBMETIDO) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        if (request.status == RequestStatus.REJEITADO) {
                            // --- REJECTED STATE ---
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
                            // --- PENDING_LEVANTAMENTO OR CONCLUIDO STATE ---
                            // Change label based on history vs upcoming
                            val label = if (request.status == RequestStatus.CONCLUIDO)
                                "Levantado em" else "Data de levantamento"

                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = request.pickupDate ?: "Data a definir",
                                fontSize = 15.sp,
                                color = BodyText
                            )
                            if (request.pickupTimeRange != null) {
                                Text(
                                    text = request.pickupTimeRange,
                                    fontSize = 15.sp,
                                    color = BodyText
                                )
                            }
                        }
                    }
                    Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                }

                // 4. Footer (Button)
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
                            text = "Ok",
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

// --- Previews ---

@Preview(name = "Accepted/Pending Request")
@Composable
fun PreviewDialogAccepted() {
    val details = RequestDetails(
        status = RequestStatus.PENDENTE_LEVANTAMENTO, // Updated Enum Name
        products = listOf(
            "1x Detergente da loiça",
            "4x Pacote de arroz",
            "4x Pacote de massa"
        ),
        pickupDate = "Segunda-feira, 2 de Março de 2026",
        pickupTimeRange = "Entre as 14h00 e 18h00"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        RequestDetailsDialog(request = details, onDismiss = {})
    }
}

@Preview(name = "Rejected Request")
@Composable
fun PreviewDialogRejected() {
    val details = RequestDetails(
        status = RequestStatus.REJEITADO,
        products = listOf(
            "2x Óleo Alimentar",
            "1x Leite UHT"
        ),
        rejectionReason = "Infelizmente não temos stock suficiente."
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        RequestDetailsDialog(request = details, onDismiss = {})
    }
}

@Preview(name = "Submitted Request")
@Composable
fun PreviewDialogSubmitted() {
    val details = RequestDetails(
        status = RequestStatus.SUBMETIDO,
        products = listOf(
            "1x Cabaz Básico"
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        RequestDetailsDialog(request = details, onDismiss = {})
    }
}