package com.lojasocial.app.ui.requestitems.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.InactiveFilterBackground
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextDarkGray

@Composable
fun CategoryFilters(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Button(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) LojaSocialPrimary else InactiveFilterBackground,
                    contentColor = if (isSelected) Color.White else TextDarkGray
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = category,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}