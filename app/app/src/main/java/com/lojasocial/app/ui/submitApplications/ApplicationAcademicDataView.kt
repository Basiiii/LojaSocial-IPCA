package com.lojasocial.app.ui.submitApplications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.lojasocial.app.ui.submitApplications.components.ApplicationHeader
import com.lojasocial.app.ui.submitApplications.components.CustomLabelInput
import com.lojasocial.app.ui.submitApplications.components.DropdownInputField
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel

/**
 * Second page of the scholarship application form - Academic Information.
 * 
 * This composable displays the academic information section of the scholarship
 * application form. It collects details about the applicant's academic pursuits,
 * including degree program, student identification, and financial support status.
 * 
 * Features:
 * - Collects academic information (degree, course, student number)
 * - Captures financial support details (FAES support, other scholarships)
 * - Provides dropdown selections for standardized options
 * - Maintains form state using ViewModel with StateFlow
 * - Supports bidirectional navigation between form pages
 * - Uses Portuguese labels and options for user interface
 * - Handles Boolean to String conversion for dropdown compatibility
 * 
 * @param onNavigateBack Callback for navigating to previous form page
 * @param onNavigateNext Callback for navigating to next form page
 * @param viewModel ViewModel for managing form state and business logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaAcademicDataView(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    /**
     * Collects the current form data from the ViewModel.
     */
    val formData by viewModel.formData.collectAsState()

    /**
     * Local state for storing the selected academic degree.
     */
    var academicDegree by remember { mutableStateOf(formData.academicDegree) }

    /**
     * Local state for storing the selected course.
     */
    var course by remember { mutableStateOf(formData.course) }

    /**
     * Local state for storing the student number.
     */
    var studentNumber by remember { mutableStateOf(formData.studentNumber) }

    /**
     * Local state for storing FAES support status (converted from Boolean to "Sim"/"Não").
     */
    var faesSupport by remember { mutableStateOf(formData.faesSupport?.let { if (it) "Sim" else "Não" } ?: "") }

    /**
     * Local state for storing scholarship status (converted from Boolean to "Sim"/"Não").
     */
    var hasScholarship by remember { mutableStateOf(formData.hasScholarship?.let { if (it) "Sim" else "Não" } ?: "") }
    
    /**
     * Tracks if validation has been attempted (only show errors after button click).
     */
    var validationAttempted by remember { mutableStateOf(false) }

    /**
     * Synchronizes local state with ViewModel data when form data changes.
     * This ensures consistency between UI state and ViewModel state.
     */
    LaunchedEffect(formData) {
        academicDegree = formData.academicDegree
        course = formData.course
        studentNumber = formData.studentNumber
        faesSupport = formData.faesSupport?.let { if (it) "Sim" else "Não" } ?: ""
        hasScholarship = formData.hasScholarship?.let { if (it) "Sim" else "Não" } ?: ""
    }

    /**
     * Available academic degree options for the dropdown.
     */
    val degreesList = listOf("Licenciatura", "Mestrado", "CTeSP", "Pós-Graduação")

    /**
     * Available course options for the dropdown.
     */
    val coursesList = listOf("Engenharia Informática", "Design Gráfico", "Gestão", "Solicitadoria")

    /**
     * Standard yes/no options for boolean questions.
     */
    val yesNoList = listOf("Sim", "Não")

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
                    IconButton(onClick = onNavigateBack) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonGray,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Anterior", fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            validationAttempted = true
                            // Validate before navigating
                            if (academicDegree.isNotBlank() && course.isNotBlank() && 
                                studentNumber.isNotBlank() && faesSupport.isNotBlank() && 
                                hasScholarship.isNotBlank()) {
                                onNavigateNext()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
                    ) {
                        Text("Seguinte", fontSize = 16.sp)
                    }
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
                title = "Dados académicos",
                pageNumber = "Página 2 de 3"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val academicDegreeError = if (validationAttempted && academicDegree.isBlank()) "Grau académico é obrigatório" else null
                DropdownInputField(
                    label = "Grau Académico",
                    value = academicDegree,
                    options = degreesList,
                    onValueChange = {
                        academicDegree = it
                        viewModel.academicDegree = it
                    }
                )
                if (academicDegreeError != null) {
                    Text(
                        text = academicDegreeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = (-12).dp, bottom = 8.dp)
                    )
                }

                val courseError = if (validationAttempted && course.isBlank()) "Curso é obrigatório" else null
                DropdownInputField(
                    label = "Curso",
                    value = course,
                    options = coursesList,
                    onValueChange = {
                        course = it
                        viewModel.course = it
                    }
                )
                if (courseError != null) {
                    Text(
                        text = courseError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = (-12).dp, bottom = 8.dp)
                    )
                }

                val studentNumberError = if (validationAttempted && studentNumber.isBlank()) "Número de estudante é obrigatório" else null
                CustomLabelInput(
                    label = "Número de Estudante",
                    value = studentNumber,
                    onValueChange = {
                        studentNumber = it
                        viewModel.studentNumber = it
                    },
                    placeholder = "Introduz número de estudante",
                    errorMessage = studentNumberError,
                    isError = studentNumberError != null
                )

                val faesSupportError = if (validationAttempted && faesSupport.isBlank()) "Informação sobre apoio FAES é obrigatória" else null
                DropdownInputField(
                    label = "É apoiado(a) pelo Fundo de Apoio de Emergência Social (FAES)?",
                    value = faesSupport,
                    options = yesNoList,
                    onValueChange = {
                        faesSupport = it
                        viewModel.faesSupport = it == "Sim"
                    }
                )
                if (faesSupportError != null) {
                    Text(
                        text = faesSupportError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = (-12).dp, bottom = 8.dp)
                    )
                }

                val hasScholarshipError = if (validationAttempted && hasScholarship.isBlank()) "Informação sobre bolsa é obrigatória" else null
                DropdownInputField(
                    label = "É beneficiário de alguma bolsa de estudo ou apoio no presente ano letivo?",
                    value = hasScholarship,
                    options = yesNoList,
                    onValueChange = {
                        hasScholarship = it
                        viewModel.hasScholarship = it == "Sim"
                    }
                )
                if (hasScholarshipError != null) {
                    Text(
                        text = hasScholarshipError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = (-12).dp, bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun CandidaturaStep2Preview() {
    MaterialTheme {
        CandidaturaAcademicDataView( onNavigateBack = {}, onNavigateNext = {}, viewModel = hiltViewModel())
    }
}