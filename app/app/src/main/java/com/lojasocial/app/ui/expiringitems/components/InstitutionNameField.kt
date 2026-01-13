package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.BorderColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun InstitutionNameField(
    institutionName: String,
    onInstitutionNameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = institutionName,
        onValueChange = onInstitutionNameChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Nome da Instituição", color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = TextGray) },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = BorderColor,
            focusedBorderColor = LojaSocialPrimary,
            cursorColor = LojaSocialPrimary
        ),
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
fun InstitutionNameFieldPreview() {
    MaterialTheme {
        InstitutionNameField(
            institutionName = "",
            onInstitutionNameChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InstitutionNameFieldWithTextPreview() {
    MaterialTheme {
        InstitutionNameField(
            institutionName = "Cruz Vermelha",
            onInstitutionNameChange = {}
        )
    }
}
