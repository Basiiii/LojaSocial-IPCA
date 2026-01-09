package com.lojasocial.app.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val imageUrl: String,
    val stockQuantity: Int,
    val status: ProductStatus
)

data class ProductStatus(
    val label: String,
    val color: Color,
    val bgColor: Color
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen() {
    val inStock = ProductStatus("Em stock", Color(0xFF1B5E20), Color(0xFFDFF7E2))
    val lowStock = ProductStatus("Stock baixo", Color(0xFFC86E39), Color(0xFFFCECDD))
    val outOfStock = ProductStatus("Esgotado", Color(0xFFD32F2F), Color(0xFFFFDAD4))
    
    val products = listOf(
        Product(1, "Arroz Carolino 1kg", "Alimentar", "https://example.com/arroz.jpg", 45, inStock),
        Product(2, "Massa Esparguete 500g", "Alimentar", "https://example.com/massa.jpg", 12, inStock),
        Product(3, "Detergente Multiusos 500ml", "Limpeza", "https://example.com/detergente.jpg", 3, lowStock),
        Product(4, "Água Mineral Natural 1L", "Alimentar", "https://example.com/agua.jpg", 120, inStock),
        Product(5, "Lata de Atum em posta", "Alimentar", "https://example.com/atum.jpg", 0, outOfStock),
        Product(6, "Desodorizante Spray 150ml", "Higiene", "https://example.com/deo.jpg", 8, lowStock),
        Product(7, "Gel de Banho 750ml", "Higiene", "https://example.com/gel.jpg", 22, inStock),
        Product(8, "Pack Leite Meio Gordo 6x1L", "Alimentar", "https://example.com/leite.jpg", 0, outOfStock),
        Product(9, "Lixívia Tradicional 2L", "Limpeza", "https://example.com/lixivia.jpg", 15, inStock)
    )

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Catálogo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            FilterHeader(productCount = products.size)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    ProductCard(product = product)
                }
            }
        }
    }
}


@Composable
fun FilterHeader(productCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$productCount Produtos",
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = Color(0xFF2D75F0),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Filtrar & Ordenar",
                color = Color(0xFF2D75F0),
                fontWeight = FontWeight.Medium
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
}

@Composable
fun ProductCard(product: Product) {
    val cardAlpha = if (product.status.label == "Esgotado") 0.6f else 1f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = cardAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min)
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Restaurant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.category.uppercase(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black.copy(alpha = if (product.status.label == "Esgotado") 0.7f else 1f)
                    )
                }

                Surface(
                    color = product.status.bgColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "${product.status.label} (${product.stockQuantity})",
                        color = product.status.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProductList() {
    MaterialTheme {
        ProductListScreen()
    }
}