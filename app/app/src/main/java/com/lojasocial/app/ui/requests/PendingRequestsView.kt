package com.lojasocial.app.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.R
import com.lojasocial.app.domain.request.PendingRequest
import com.lojasocial.app.ui.requests.components.RequestItem
import com.lojasocial.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsView(
    uiState: PendingRequestsUiState,
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Pedidos Pendentes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = TextDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = LojaSocialSurface
                    )
                )
            }
        },
        containerColor = LojaSocialBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (uiState) {
                is PendingRequestsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PendingRequestsUiState.Success -> {
                    val requests = uiState.requests
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(BrandOrange, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${requests.size} Pendentes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Atualizado agora",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(requests) { request ->
                                RequestItem(
                                    request = request,
                                    onClick = { }
                                )
                                HorizontalDivider(color = LojaSocialBackground, thickness = 1.dp)
                            }
                        }
                    }
                }
                is PendingRequestsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Success State")
@Composable
fun PendingRequestsPreview_Success() {
    val fakeRequests = listOf(
        PendingRequest(1, "José Alves", "2h atrás", "Vários", R.drawable.basket_fill),
        PendingRequest(2, "Enrique Rodrigues", "3h atrás", "Limpeza", R.drawable.mop),
        PendingRequest(3, "Diogo Machado", "8h atrás", "Alimentar", R.drawable.cutlery),
        PendingRequest(4, "Carlos Barreiro", "14h atrás", "Higiene", R.drawable.perfume)
    )
    MaterialTheme {
        PendingRequestsView(uiState = PendingRequestsUiState.Success(fakeRequests))
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun PendingRequestsPreview_Loading() {
    MaterialTheme {
        PendingRequestsView(uiState = PendingRequestsUiState.Loading)
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun PendingRequestsPreview_Error() {
    MaterialTheme {
        PendingRequestsView(uiState = PendingRequestsUiState.Error("Não foi possível carregar os pedidos"))
    }
}