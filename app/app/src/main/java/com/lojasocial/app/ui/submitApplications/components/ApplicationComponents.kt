package com.lojasocial.app.ui.submitApplications.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.LojaSocialPrimary

/**
 * Gray color used for text elements in the application form.
 * 
 * This color provides better readability and visual hierarchy
 * for labels and secondary text elements.
 */
val TextGray = Color(0xFF455A64)

/**
 * Custom text input field with label for the application form.
 * 
 * This composable provides a standardized input field with a label,
 * placeholder text, and consistent styling across the application.
 * It supports different keyboard types and maintains the app's visual theme.
 * 
 * @param label The descriptive label displayed above the input field
 * @param value The current value of the input field
 * @param onValueChange Callback invoked when the input value changes
 * @param placeholder The placeholder text displayed when the field is empty
 * @param keyboardType The type of keyboard to show for this input field
 * @param errorMessage Optional error message to display below the field
 * @param isError Whether the field is in an error state
 */
@Composable
fun CustomLabelInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    isError: Boolean = false
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.LightGray) },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            supportingText = errorMessage?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color.LightGray,
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else LojaSocialPrimary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

/**
 * Phone number input field with Portuguese country code prefix.
 * 
 * This composable provides a specialized input field for phone numbers
 * with the Portuguese country code (+351) pre-fixed. It uses phone keyboard
 * type for better user experience on mobile devices.
 * 
 * @param value The current phone number value (without country code)
 * @param onValueChange Callback invoked when the phone number changes
 * @param placeholder Placeholder text for the phone input field
 * @param errorMessage Optional error message to display below the field
 * @param isError Whether the field is in an error state
 */
@Composable
fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Insira o seu numero aqui",
    errorMessage: String? = null,
    isError: Boolean = false
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "Telemóvel",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            prefix = {
                Text(
                    "+351 ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = isError,
            supportingText = errorMessage?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color.LightGray,
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else LojaSocialPrimary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

/**
 * Header component for application form pages.
 * 
 * This composable provides a consistent header layout for all pages
 * of the scholarship application form, displaying the page title
 * and current page number for user orientation.
 * 
 * @param title The title of the current application page
 * @param pageNumber The current page number (e.g., "Página 1 de 3")
 */
@Composable
fun ApplicationHeader(
    title: String,
    pageNumber: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.lojasocial.app.ui.theme.LojaSocialHeaderBlue)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextGray
            )
        )
        Text(
            text = pageNumber,
            style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInputField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    placeholder: String = "Selecione uma opção"
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                placeholder = { Text(placeholder, color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = LojaSocialPrimary
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
