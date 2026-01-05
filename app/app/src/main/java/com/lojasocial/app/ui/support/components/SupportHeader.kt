package com.lojasocial.app.ui.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGrey

/**
 * Top bar component for the Support screen.
 * 
 * This component provides a consistent header for the support section
 * with optional back navigation functionality.
 * 
 * @param title The title to display in the top bar.
 * @param showBackButton Whether to show the back navigation button.
 * @param onNavigateBack Callback when the back button is pressed.
 * @param modifier Optional modifier for styling and layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTopBar(
    modifier: Modifier = Modifier,
    title: String = "Suporte",
    showBackButton: Boolean = true,
    onNavigateBack: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        navigationIcon = {
            if (showBackButton && onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextDark
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        ),
        modifier = modifier
    )
}

/**
 * Section header component for support content sections.
 * 
 * This component provides a consistent header format for different
 * sections within the support screen (FAQs, Contact, etc.).
 * 
 * @param title The section title.
 * @param subtitle Optional subtitle providing additional context.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun SupportSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        subtitle?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = TextGrey,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
