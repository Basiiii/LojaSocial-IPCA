package com.lojasocial.app.ui.support.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.domain.support.FaqItem

/**
 * Individual FAQ accordion item component.
 * 
 * This component displays a single FAQ item with expandable functionality.
 * When clicked, it reveals the answer with a smooth animation and rotates
 * the chevron icon to indicate the expanded state.
 * 
 * @param data The FAQ item containing question and answer.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun FaqAccordionItem(
    data: FaqItem,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Color(0xFFF8F9FA) else Color.White
        ),
        border = if (expanded) null else BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.question,
                    fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                    color = if (expanded) LojaSocialPrimary else TextDark,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationState),
                    tint = if (expanded) LojaSocialPrimary else Color.Gray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = data.answer,
                    fontSize = 14.sp,
                    color = TextGrey,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * A section component that displays a list of FAQ accordion items.
 * 
 * This component renders a collection of FAQ items with proper spacing
 * and layout for the support screen.
 * 
 * @param faqList List of FAQ items to display.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun FaqSection(
    faqList: List<FaqItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        faqList.forEachIndexed { index, faq ->
            FaqAccordionItem(data = faq)
            if (index < faqList.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
