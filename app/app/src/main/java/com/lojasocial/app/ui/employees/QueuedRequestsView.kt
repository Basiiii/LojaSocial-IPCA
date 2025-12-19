package com.lojasocial.app.ui.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.R

// Cores ajustadas para maior fidelidade ao design
val ColorHeaderBg = Color(0xFFF0F9FF) // Azul suave do banner
val ColorBadgeOrangeBg = Color(0xFFFFFAEB)
val ColorBadgeOrangeText = Color(0xFFB54708)
val ColorTextName = Color(0xFF101828)
val ColorTextDetails = Color(0xFF667085)
val ColorDivider = Color(0xFFF2F4F7)
val ColorArrow = Color(0xFFD0D5DD)

data class QueuedRequest(
    val id: Int,
    val name: String,
    val time: String,
    val category: String,
    val categoryIcon: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuedRequestsView(
    onBack: () -> Unit = {}
) {
    val requests = listOf(
        QueuedRequest(1, "José Alves", "2h atrás", "Vários", R.drawable.basket_fill),
        QueuedRequest(2, "Enrique Rodrigues", "3h atrás", "Limpeza", R.drawable.mop),
        QueuedRequest(3, "Diogo Machado", "8h atrás", "Alimentar", R.drawable.cutlery),
        QueuedRequest(4, "Carlos Barreiro", "14h atrás", "Higiene", R.drawable.perfume)
    )

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Pedidos Pendentes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ColorTextName
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = ColorTextName
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
                // Subheader (Banner de status)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorHeaderBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFF79009), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${requests.size} Pendentes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextDetails
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Atualizado há 2 minutos",
                        fontSize = 12.sp,
                        color = ColorTextDetails
                    )
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(requests) { request ->
                RequestItem(request = request)
                HorizontalDivider(color = ColorDivider, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun RequestItem(request: QueuedRequest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically // Garante que avatar, texto e seta alinhem pelo centro
    ) {
        // Avatar Placeholder (Como solicitado, mantido, mas com cor suave)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAECF0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Conteúdo central (Nome, Tempo e Badge)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, // Peso de fonte mais próximo do print
                    color = ColorTextName
                )
                Text(
                    text = request.time,
                    fontSize = 13.sp,
                    color = ColorTextDetails
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge()
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    painter = painterResource(id = request.categoryIcon),
                    contentDescription = null,
                    tint = ColorTextDetails,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.category,
                    fontSize = 13.sp,
                    color = ColorTextDetails
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Seta de navegação (corrigida a posição para o centro da Row principal)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ColorArrow,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun StatusBadge() {
    Surface(
        color = ColorBadgeOrangeBg,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = ColorBadgeOrangeText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Pendente",
                color = ColorBadgeOrangeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QueuedRequestsPreview() {
    MaterialTheme {
        QueuedRequestsView()
    }
}