package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = java.util.Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 60 * 1000 -> "Há poucos minutos"
        diff < 24 * 60 * 60 * 1000 -> "Há ${diff / (60 * 60 * 1000)} horas"
        diff < 7 * 24 * 60 * 60 * 1000 -> "Há ${diff / (24 * 60 * 60 * 1000)} dias"
        else -> "Há mais de uma semana"
    }
}

data class Institution(
    val id: String = "",
    val name: String = "",
    val lastPickup: com.google.firebase.Timestamp? = null
)

@Composable
fun InstitutionDropdownField(
    institutions: List<Institution>,
    selectedInstitution: String,
    onInstitutionChange: (String) -> Unit,
    isLoading: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(selectedInstitution) }

    LaunchedEffect(selectedInstitution) {
        textValue = selectedInstitution
    }

    // Filter institutions based on current text input
    val filteredInstitutions = remember(textValue, institutions) {
        if (textValue.isBlank()) {
            institutions
                .filter { it.lastPickup != null }
                .sortedByDescending { it.lastPickup?.toDate() }
                .take(3)
        } else {
            institutions
                .filter { institution ->
                    institution.name.contains(textValue, ignoreCase = true)
                }
                .sortedByDescending { it.lastPickup?.toDate() }
                .take(3)
        }
        .distinctBy { it.name.lowercase().trim() }
    }

    // Auto-expand dropdown when typing and there are matches
    LaunchedEffect(textValue, filteredInstitutions) {
        if (textValue.isNotBlank() && filteredInstitutions.isNotEmpty()) {
            expanded = true
        } else if (textValue.isBlank()) {
            expanded = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Dropdown field
        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                onInstitutionChange(it)
                // Don't close dropdown when typing - let it auto-expand based on matches
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = "Nome da Instituição", 
                    color = TextGray,
                    fontSize = 16.sp
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Business, 
                    contentDescription = null, 
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                ) 
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    tint = TextGray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { expanded = !expanded }
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = BorderColor,
                focusedBorderColor = LojaSocialPrimary,
                cursorColor = LojaSocialPrimary
            ),
            singleLine = true,
            readOnly = false // Allow typing
        )

        // Dropdown menu
        if (expanded && filteredInstitutions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show search results when typing
                    if (textValue.isNotBlank()) {
                        Text(
                            text = "Resultados da Pesquisa:",
                            modifier = Modifier.padding(12.dp),
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Show filtered institutions
                        filteredInstitutions.forEach { institution ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        textValue = institution.name
                                        onInstitutionChange(institution.name)
                                        expanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = institution.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    institution.lastPickup?.let { timestamp ->
                                        Text(
                                            text = "Última recolha: ${formatTimestamp(timestamp)}",
                                            color = TextGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Show recent institutions when field is empty and dropdown is manually opened
                        Text(
                            text = "Instituições Recentes:",
                            modifier = Modifier.padding(12.dp),
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Show recent institutions (max 3)
                        filteredInstitutions.forEach { institution ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        textValue = institution.name
                                        onInstitutionChange(institution.name)
                                        expanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = institution.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    institution.lastPickup?.let { timestamp ->
                                        Text(
                                            text = "Última recolha: ${formatTimestamp(timestamp)}",
                                            color = TextGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InstitutionDropdownFieldPreview() {
    MaterialTheme {
        InstitutionDropdownField(
            institutions = listOf(
                Institution(
                    id = "1",
                    name = "Cruz Vermelha",
                    lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                ),
                Institution(
                    id = "2", 
                    name = "Banco Alimentar",
                    lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                ),
                Institution(
                    id = "3",
                    name = "Caritas",
                    lastPickup = null
                )
            ),
            selectedInstitution = "",
            onInstitutionChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Dropdown Aberto")
@Composable
fun InstitutionDropdownFieldOpenedPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Instituições Recentes:",
                        modifier = Modifier.padding(12.dp),
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    listOf(
                        Institution(
                            id = "1",
                            name = "Cruz Vermelha",
                            lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 30 * 60 * 1000))
                        ),
                        Institution(
                            id = "2", 
                            name = "Banco Alimentar",
                            lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                        ),
                        Institution(
                            id = "3",
                            name = "Caritas Portugal",
                            lastPickup = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000))
                        )
                    ).forEach { institution ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = institution.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                institution.lastPickup?.let { timestamp ->
                                    Text(
                                        text = "Última recolha: ${formatTimestamp(timestamp)}",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}