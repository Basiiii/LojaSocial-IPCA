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
 * An expandable accordion component for displaying FAQ items.
 * 
 * This component shows a question that can be clicked to reveal the answer.
 * It features smooth animations for the expand/collapse action and includes
 * a rotating arrow indicator.
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

    // Rotation animation for the arrow indicator
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Arrow Rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, LightBorder),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Question header with expand/collapse indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = data.question,
                fontWeight = FontWeight.Medium,
                color = TextGrey,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextGrey,
                modifier = Modifier.rotate(rotationState)
            )
        }

        // Animated answer content
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = LightBorder.copy(alpha = 0.5f)
                )
                Text(
                    text = data.answer,
                    fontSize = 14.sp,
                    color = TextDark,
                    lineHeight = 20.sp
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
