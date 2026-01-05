package com.lojasocial.app.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.lojasocial.app.ui.applications.components.ApplicationHeader
import com.lojasocial.app.ui.applications.components.CustomLabelInput
import com.lojasocial.app.ui.applications.components.PhoneInputField
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * First page of the scholarship application form - Personal Information.
 * 
 * This composable displays the personal information section of the scholarship
 * application form. It collects essential personal details including name,
 * date of birth, identification documents, and contact information.
 * 
 * Features:
 * - Collects personal information (name, date of birth, ID/Passport)
 * - Captures contact details (email, phone)
 * - Provides date picker for date of birth selection
 * - Maintains form state using ViewModel with StateFlow
 * - Supports navigation between form pages
 * - Uses Portuguese labels and placeholders for user interface
 * 
 * @param navController Navigation controller for app navigation
 * @param onNavigateBack Callback for navigating to previous screen
 * @param onNavigateNext Callback for navigating to next form page
 * @param viewModel ViewModel for managing form state and business logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaPersonalInfoView(
    navController: NavController = rememberNavController(),
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    /**
     * Collects the current form data from the ViewModel.
     */
    val formData by viewModel.formData.collectAsState()

    /**
     * Local state for storing the user's name.
     */
    var name by remember { mutableStateOf(formData.name) }

    /**
     * Local state for storing the user's date of birth.
     */
    var dateOfBirth by remember { mutableStateOf(formData.dateOfBirth) }

    /**
     * Local state for storing the user's ID or Passport number.
     */
    var idPassport by remember { mutableStateOf(formData.idPassport) }

    /**
     * Local state for storing the user's email address.
     */
    var email by remember { mutableStateOf(formData.email) }
    var phone by remember { mutableStateOf(formData.phone) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(formData) {
        name = formData.name
        dateOfBirth = formData.dateOfBirth
        idPassport = formData.idPassport
        email = formData.email
        phone = formData.phone
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Realizar Candidatura",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("nonBeneficiaryPortal") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
                ) {
                    Text("Seguinte", fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ApplicationHeader(
                title = "Informações Pessoais",
                pageNumber = "Página 1 de 3"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                CustomLabelInput(
                    label = "Nome",
                    value = name,
                    onValueChange = {
                        name = it
                        viewModel.name = it
                    },
                    placeholder = "Insira o nome completo"
                )

                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "Data de nascimento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    OutlinedTextField(
                        value = dateOfBirth?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        onValueChange = { },
                        placeholder = { Text("dd/mm/aaaa", color = Color.LightGray) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Select date",
                                    tint = Color.Gray
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { showDatePicker = true }
                            ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = LojaSocialPrimary
                        )
                    )
                }

                CustomLabelInput(
                    label = "CC/Passaporte",
                    value = idPassport,
                    onValueChange = {
                        idPassport = it
                        viewModel.idPassport = it
                    },
                    placeholder = "Introduz número de CC ou Passaporte"
                )

                CustomLabelInput(
                    label = "Email",
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.email = it
                    },
                    placeholder = "Insira aqui o seu email",
                    keyboardType = KeyboardType.Email
                )

                PhoneInputField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        viewModel.phone = it
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val selectedDate = Date(millis)
                                    dateOfBirth = selectedDate
                                    viewModel.dateOfBirth = selectedDate
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun CandidaturaStep1Preview() {
    MaterialTheme {
        CandidaturaPersonalInfoView(
            navController = rememberNavController(),
            onNavigateBack = {},
            onNavigateNext = {}
        )
    }
}