package com.lojasocial.app.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.lojasocial.app.data.model.RequestStatus
import com.lojasocial.app.ui.components.RequestItemCard

// --- Data Model for the List ---
data class RequestItem(
    val status: RequestStatus,
    val title: String,
    val description: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupRequestsView(
    onNavigateBack: () -> Unit
) {
    // Mock Data based on your image
    val requests = listOf(
        RequestItem(
            status = RequestStatus.SUBMETIDO,
            title = "Pedido Submetido",
            description = "Pedido submetido e pendente",
            time = "há 1 hora"
        ),
        RequestItem(
            status = RequestStatus.CONCLUIDO,
            title = "Levantamento Concluído",
            description = "Levantamento feito com sucesso",
            time = "há 3 semanas"
        ),
        RequestItem(
            status = RequestStatus.REJEITADO,
            title = "Pedido Rejeitado",
            description = "O seu pedido não foi aceite. Clique para ver detalhes",
            time = "há 3 semanas"
        )
    )

    val pendingCount = requests.count { it.status == RequestStatus.PENDENTE_LEVANTAMENTO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pedidos de Levantamento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            // --- Info Bar (Gray Strip) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6)) // Light Gray bg
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pending Count with Orange Dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)) // Orange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$pendingCount Pendente${if (pendingCount != 1) "s" else ""}",
                        color = Color(0xFF374151),
                        fontSize = 14.sp
                    )
                }

                // Update Time
                Text(
                    text = "Atualizado há 2 minutos",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }

            // --- Divider ---
            Divider(color = Color(0xFFE5E7EB))

            // --- List of Requests ---
            LazyColumn {
                items(requests.size) { index ->
                    val item = requests[index]

                    RequestItemCard(
                        status = item.status,
                        title = item.title,
                        subtitle = item.description,
                        time = item.time,
                        onClick = { /* Handle click to details */ }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRequests() {
    MaterialTheme {
        PickupRequestsView(onNavigateBack = {})
    }
}