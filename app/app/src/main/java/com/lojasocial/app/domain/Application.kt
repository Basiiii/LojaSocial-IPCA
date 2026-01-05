package com.lojasocial.app.domain

import android.net.Uri
import java.util.Date

data class Application(
    val id: String = "",
    val personalInfo: PersonalInfo,
    val academicInfo: AcademicInfo,
    val documents: List<ApplicationDocument>,
    val submissionDate: Date = Date(),
    val status: ApplicationStatus = ApplicationStatus.PENDING
)

data class PersonalInfo(
    val name: String,
    val dateOfBirth: Date?,
    val idPassport: String,
    val email: String,
    val phone: String
)

data class AcademicInfo(
    val academicDegree: String,
    val course: String,
    val studentNumber: String,
    val faesSupport: Boolean,
    val hasScholarship: Boolean
)

data class ApplicationDocument(
    val id: Int,
    val name: String,
    val uri: Uri?,
    val fileName: String? = null
)

enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNDER_REVIEW
}
