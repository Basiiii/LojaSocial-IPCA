package com.lojasocial.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Modelos de Dados (Para UI) ---
data class ProductUI(
    val id: Int,
    val name: String,
    val category: String,
    val stock: Int,
    var quantity: Int = 0
)

// --- Cores Personalizadas (Baseadas na imagem) ---
val GreenPrimary = Color(0xFF1B5E3F) // Verde escuro dos botões/chips
val GreenStock = Color(0xFF4CAF50)   // Verde "Disponível"
val RedStock = Color(0xFFE53935)     // Vermelho quando stock baixo
val LightBg = Color(0xFFF5F7FA)      // Fundo cinza claro
val DisabledBtn = Color(0xFFD1D5DB)  // Cinza do botão desativado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeOrderScreen(
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    // Estado simulado (num caso real viria do ViewModel)
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Todos", "Alimentar", "Limpeza", "Higiene")
    var selectedCategory by remember { mutableStateOf("Todos") }

    // Lista de produtos baseada na tua imagem
    val products = remember {
        mutableStateListOf(
            ProductUI(1, "Pacote Arroz 1Kg", "Alimentar", 25, 0),
            ProductUI(2, "Pacote Massa 0.5Kg", "Alimentar", 7, 0),
            ProductUI(3, "Detergente Multiusos 500ml", "Limpeza", 3, 0),
            ProductUI(4, "Leite UHT 1L", "Alimentar", 12, 0)
        )
    }

    // Cálculos
    val totalItemsSelected = products.sumOf { it.quantity }
    val maxItems = 10

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Fazer Pedido",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            // Botão de Submeter Fixo no fundo
            Button(
                onClick = onSubmitClick,
                enabled = totalItemsSelected > 0, // Desativado se 0 items
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    disabledContainerColor = DisabledBtn,
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
            ) {
                Text(
                    text = "Submeter Pedido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // --- 1. Cabeçalho de Limite (Status Bar) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F4F8)) // Azul/Cinza muito claro
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalItemsSelected de $maxItems artigos selecionados",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Limpar",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                        // Adicionar clickable aqui para limpar tudo se necessário
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { totalItemsSelected / maxItems.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF98FB98), // Verde claro fluorescente da imagem
                    trackColor = Color.White,
                )
            }

            // --- 2. Barra de Pesquisa ---
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisa", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = GreenPrimary
                    ),
                    singleLine = true
                )
            }

            // --- 3. Filtros (Chips) ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF5F5F5), // Cinza claro fundo
                            labelColor = Color.DarkGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if(isSelected) GreenPrimary else Color.Transparent,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // --- 4. Lista de Produtos ---
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp) // Espaço para não bater no botão
            ) {
                items(products) { product ->
                    ProductItemRow(
                        product = product,
                        onAdd = {
                            if (totalItemsSelected < maxItems && product.quantity < product.stock) {
                                val index = products.indexOf(product)
                                products[index] = product.copy(quantity = product.quantity + 1)
                            }
                        },
                        onRemove = {
                            if (product.quantity > 0) {
                                val index = products.indexOf(product)
                                products[index] = product.copy(quantity = product.quantity - 1)
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFF0F0F0)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(
    product: ProductUI,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Coluna da Esquerda (Info)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            Text(
                text = product.category,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Lógica de cor do stock (vermelho se < 5, verde se >= 5)
            val stockColor = if (product.stock < 5) RedStock else GreenStock

            Text(
                text = "Disponível: ${product.stock}",
                fontSize = 12.sp,
                color = stockColor,
                fontWeight = FontWeight.Medium
            )
        }

        // Coluna da Direita (Controles + -)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuantityButton(
                icon = Icons.Default.Remove,
                onClick = onRemove,
                enabled = product.quantity > 0
            )

            Text(
                text = product.quantity.toString(),
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            QuantityButton(
                icon = Icons.Default.Add,
                onClick = onAdd,
                enabled = true // Podes adicionar lógica de maxStock aqui
            )
        }
    }
}

@Composable
fun QuantityButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(32.dp)
            .background(
                color = GreenPrimary,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMakeOrderScreen() {
    MakeOrderScreen()
}