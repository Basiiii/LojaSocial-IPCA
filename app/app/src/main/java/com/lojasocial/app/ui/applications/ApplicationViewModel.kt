package com.lojasocial.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.Application
import com.lojasocial.app.domain.ApplicationDocument
import com.lojasocial.app.domain.ApplicationStatus
import com.lojasocial.app.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ApplicationUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val submissionError: String? = null,
    val applications: List<Application> = emptyList()
)

data class FormData(
    val name: String = "",
    val dateOfBirth: Date? = null,
    val idPassport: String = "",
    val email: String = "",
    val phone: String = "",
    val academicDegree: String = "",
    val course: String = "",
    val studentNumber: String = "",
    val faesSupport: Boolean? = null,
    val hasScholarship: Boolean? = null,
    val documents: List<ApplicationDocument> = emptyList()
)

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()
    
    // Form data stored in StateFlow to persist across navigation
    private val _formData = MutableStateFlow(
        FormData(
            name = "",
            dateOfBirth = null,
            idPassport = "",
            email = "",
            phone = "",
            academicDegree = "",
            course = "",
            studentNumber = "",
            faesSupport = null,
            hasScholarship = null,
            documents = emptyList()
        )
    )
    val formData: StateFlow<FormData> = _formData.asStateFlow()

    // Personal Info - synced with StateFlow
    var name: String
        get() = _formData.value.name
        set(value) { _formData.value = _formData.value.copy(name = value) }
    var dateOfBirth: Date?
        get() = _formData.value.dateOfBirth
        set(value) { _formData.value = _formData.value.copy(dateOfBirth = value) }
    var idPassport: String
        get() = _formData.value.idPassport
        set(value) { _formData.value = _formData.value.copy(idPassport = value) }
    var email: String
        get() = _formData.value.email
        set(value) { _formData.value = _formData.value.copy(email = value) }
    var phone: String
        get() = _formData.value.phone
        set(value) { _formData.value = _formData.value.copy(phone = value) }

    // Academic Info - synced with StateFlow
    var academicDegree: String
        get() = _formData.value.academicDegree
        set(value) { _formData.value = _formData.value.copy(academicDegree = value) }
    var course: String
        get() = _formData.value.course
        set(value) { _formData.value = _formData.value.copy(course = value) }
    var studentNumber: String
        get() = _formData.value.studentNumber
        set(value) { _formData.value = _formData.value.copy(studentNumber = value) }
    var faesSupport: Boolean?
        get() = _formData.value.faesSupport
        set(value) { _formData.value = _formData.value.copy(faesSupport = value) }
    var hasScholarship: Boolean?
        get() = _formData.value.hasScholarship
        set(value) { _formData.value = _formData.value.copy(hasScholarship = value) }

    // Documents - synced with StateFlow
    var documents: List<ApplicationDocument>
        get() = _formData.value.documents
        set(value) { _formData.value = _formData.value.copy(documents = value) }

    init {
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                applicationRepository.getApplications().collect { applications ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        applications = applications
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    submissionError = "Failed to load applications: ${e.message}"
                )
            }
        }
    }

    fun submitApplication() {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submissionError = null)

            try {
                val application = Application(
                    personalInfo = com.lojasocial.app.domain.PersonalInfo(
                        name = name,
                        dateOfBirth = dateOfBirth,
                        idPassport = idPassport,
                        email = email,
                        phone = phone
                    ),
                    academicInfo = com.lojasocial.app.domain.AcademicInfo(
                        academicDegree = academicDegree,
                        course = course,
                        studentNumber = studentNumber,
                        faesSupport = faesSupport ?: false,
                        hasScholarship = hasScholarship ?: false
                    ),
                    documents = documents,
                    submissionDate = Date(),
                    status = ApplicationStatus.PENDING
                )

                val result = applicationRepository.submitApplication(application)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionSuccess = true
                    )
                    clearForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionError = result.exceptionOrNull()?.message ?: "Submission failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submissionError = "Submission failed: ${e.message}"
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        val errors = mutableListOf<String>()

        if (name.isBlank()) errors.add("Nome é obrigatório")
        if (dateOfBirth == null) errors.add("Data de nascimento é obrigatória")
        if (idPassport.isBlank()) errors.add("CC/Passaporte é obrigatório")
        if (email.isBlank()) errors.add("Email é obrigatório")
        if (phone.isBlank()) errors.add("Telemóvel é obrigatório")

        if (academicDegree.isBlank()) errors.add("Grau académico é obrigatório")
        if (course.isBlank()) errors.add("Curso é obrigatório")
        if (studentNumber.isBlank()) errors.add("Número de estudante é obrigatório")
        if (faesSupport == null) errors.add("Informação sobre apoio FAES é obrigatória")
        if (hasScholarship == null) errors.add("Informação sobre bolsa é obrigatória")

        // Check if at least one document is uploaded
        val hasUploadedDocuments = documents.any { it.uri != null }
        if (!hasUploadedDocuments) {
            errors.add("Pelo menos um documento é obrigatório")
        }

        if (errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(submissionError = errors.joinToString("\n"))
            return false
        }

        return true
    }

    fun clearForm() {
        _formData.value = FormData()
    }

    fun clearSubmissionState() {
        _uiState.value = _uiState.value.copy(
            submissionSuccess = false,
            submissionError = null
        )
    }

    fun updateDocuments(newDocuments: List<ApplicationDocument>) {
        documents = newDocuments
    }

    fun addDocument(document: ApplicationDocument) {
        documents = documents + document
    }

    fun removeDocument(documentId: Int) {
        documents = documents.filter { it.id != documentId }
    }
    
    fun setContext(context: android.content.Context) {
        (applicationRepository as? com.lojasocial.app.repository.ApplicationRepositoryImpl)?.setContext(context)
    }
}
