package com.lojasocial.app.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.data.model.AcademicInfo
import com.lojasocial.app.data.model.Application
import com.lojasocial.app.data.model.ApplicationDocument
import com.lojasocial.app.data.model.ApplicationStatus
import com.lojasocial.app.data.model.PersonalInfo
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore implementation of the ApplicationRepository interface.
 * 
 * This class handles all scholarship application data operations using Firebase Firestore
 * as the backend data store. It manages document storage, retrieval, and file conversion
 * operations for the scholarship application system.
 * 
 * Key features:
 * - Stores applications in a dedicated collection: applications/{applicationId}
 * - Includes userId field in each application document for user association
 * - Converts document files to Base64 format for Firestore storage
 * - Provides real-time updates using Kotlin Flow
 * - Handles authentication and user-specific data isolation
 * 
 * @param firestore Firebase Firestore instance for database operations
 * @param auth Firebase Auth instance for user authentication
 * 
 * @see ApplicationRepository The interface this class implements
 * @see FileUtils Utility class for file operations
 */
@Singleton
class ApplicationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ApplicationRepository {

    /** Android context for file operations */
    private var context: Context? = null
    
    /**
     * Sets the Android context required for file operations.
     * 
     * This context is needed to access the ContentResolver for reading
     * file URIs and converting them to Base64 format.
     * 
     * @param context The Android application context
     */
    override fun setContext(context: Context) {
        this.context = context
    }

    /**
     * Submits a new scholarship application to Firestore.
     * 
     * This method performs the complete submission process:
     * 1. Validates user authentication
     * 2. Generates a unique application ID
     * 3. Converts all document files to Base64 format
     * 4. Stores the application data in Firestore
     * 5. Returns the application ID on success
     * 
     * @param application The complete application to submit
     * @return Result containing the application ID if successful, or error if failed
     */
    override suspend fun submitApplication(application: Application): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Create application document in applications collection
            val applicationsRef = firestore.collection("applications")
            
            val applicationWithId = application.copy(
                id = applicationsRef.document().id,
                userId = userId
            )
            
            // Convert documents to base64 and store in Firestore
            val documentsData = application.documents.map { doc ->
                val (base64Data, fileName) = doc.uri?.let { uri ->
                    context?.let { ctx ->
                        val base64 = FileUtils.convertFileToBase64(ctx, uri).getOrNull()
                        val name = FileUtils.getFileName(ctx, uri)
                        Pair(base64, name)
                    } ?: Pair(null, null)
                } ?: Pair(null, null)
                
                mapOf(
                    "id" to doc.id,
                    "name" to doc.name,
                    "fileName" to (fileName ?: doc.fileName ?: ""),
                    "base64Data" to (base64Data ?: "")
                )
            }
            
            val applicationData = mapOf(
                "id" to applicationWithId.id,
                "userId" to userId,
                "personalInfo" to mapOf(
                    "name" to application.personalInfo.name,
                    "dateOfBirth" to application.personalInfo.dateOfBirth,
                    "idPassport" to application.personalInfo.idPassport,
                    "email" to application.personalInfo.email,
                    "phone" to application.personalInfo.phone
                ),
                "academicInfo" to mapOf(
                    "academicDegree" to application.academicInfo.academicDegree,
                    "course" to application.academicInfo.course,
                    "studentNumber" to application.academicInfo.studentNumber,
                    "faesSupport" to application.academicInfo.faesSupport,
                    "hasScholarship" to application.academicInfo.hasScholarship
                ),
                "documents" to documentsData,
                "submissionDate" to application.submissionDate,
                "status" to application.status.value.toLong(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            applicationsRef
                .document(applicationWithId.id)
                .set(applicationData)
                .await()

            Result.success(applicationWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a file URI to Base64 encoded string.
     * 
     * This method uses the FileUtils class to convert local file URIs
     * to Base64 strings that can be stored in Firestore documents.
     * 
     * @param uri The URI of the file to convert
     * @return Result containing the Base64 string if successful, or error if failed
     */
    override suspend fun convertFileToBase64(uri: Uri): Result<String> {
        return try {
            context?.let { ctx ->
                FileUtils.convertFileToBase64(ctx, uri)
            } ?: Result.failure(Exception("Context not set"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all applications for the current authenticated user.
     * 
     * This method fetches all applications from the applications collection
     * in Firestore filtered by userId and converts them to domain objects. 
     * The data is mapped from the Firestore document structure to the Application domain model.
     * 
     * Firestore structure: applications/{applicationId} with userId field
     * 
     * @return Flow emitting a list of applications for the current user
     */
    override fun getApplications(): Flow<List<Application>> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit(emptyList())
                return@flow
            }
            
            val snapshot = firestore.collection("applications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val personalInfoData = data["personalInfo"] as? Map<String, Any>
                    val academicInfoData = data["academicInfo"] as? Map<String, Any>
                    val documentsData = data["documents"] as? List<Map<String, Any>>
                    
                    // Helper function to convert Firestore date to Date
                    fun convertToDate(value: Any?): Date? {
                        return when {
                            value is Date -> value
                            value != null && value.javaClass.name == "com.google.firebase.Timestamp" -> {
                                // Use reflection to call toDate() method
                                try {
                                    val toDateMethod = value.javaClass.getMethod("toDate")
                                    toDateMethod.invoke(value) as? Date
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            value != null && value.javaClass.name == "com.google.firebase.firestore.Timestamp" -> {
                                // Use reflection to call toDate() method
                                try {
                                    val toDateMethod = value.javaClass.getMethod("toDate")
                                    toDateMethod.invoke(value) as? Date
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            else -> null
                        }
                    }
                    
                    Application(
                        id = data["id"] as? String ?: doc.id,
                        userId = data["userId"] as? String ?: "",
                        personalInfo = PersonalInfo(
                            name = personalInfoData?.get("name") as? String ?: "",
                            dateOfBirth = convertToDate(personalInfoData?.get("dateOfBirth")),
                            idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                            email = personalInfoData?.get("email") as? String ?: "",
                            phone = personalInfoData?.get("phone") as? String ?: ""
                        ),
                        academicInfo = AcademicInfo(
                            academicDegree = academicInfoData?.get("academicDegree") as? String
                                ?: "",
                            course = academicInfoData?.get("course") as? String ?: "",
                            studentNumber = academicInfoData?.get("studentNumber") as? String ?: "",
                            faesSupport = when (val faes = academicInfoData?.get("faesSupport")) {
                                is Boolean -> faes
                                is String -> faes == "Sim"
                                else -> false
                            },
                            hasScholarship = when (val scholarship =
                                academicInfoData?.get("hasScholarship")) {
                                is Boolean -> scholarship
                                is String -> scholarship == "Sim"
                                else -> false
                            }
                        ),
                        documents = documentsData?.map { docData ->
                            ApplicationDocument(
                                id = (docData["id"] as? Long)?.toInt() ?: 0,
                                name = docData["name"] as? String ?: "",
                                uri = null, // Stored as base64
                                fileName = docData["fileName"] as? String,
                                base64Data = docData["base64Data"] as? String // Store base64 for viewing
                            )
                        } ?: emptyList(),
                        submissionDate = when (val dateValue = data["submissionDate"]) {
                            is Date -> dateValue
                            else -> {
                                // Try to convert Firestore Timestamp to Date using reflection
                                if (dateValue != null) {
                                    val className = dateValue.javaClass.name
                                    if (className == "com.google.firebase.Timestamp" || className == "com.google.firebase.firestore.Timestamp") {
                                        try {
                                            val toDateMethod = dateValue.javaClass.getMethod("toDate")
                                            (toDateMethod.invoke(dateValue) as? Date) ?: Date()
                                        } catch (e: Exception) {
                                            Date()
                                        }
                                    } else {
                                        Date()
                                    }
                                } else {
                                    Date()
                                }
                            }
                        },
                        status = when (val statusValue = data["status"]) {
                            is Int -> ApplicationStatus.fromInt(statusValue)
                            is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                            is String -> ApplicationStatus.fromString(statusValue)
                            else -> ApplicationStatus.PENDING
                        },
                        rejectionMessage = data["rejectionMessage"] as? String
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(applications)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * Retrieves all applications from all users.
     * 
     * This method fetches all applications from the applications collection
     * in Firestore without filtering by userId. The data is mapped from the
     * Firestore document structure to the Application domain model.
     * Uses a snapshot listener for real-time updates.
     * 
     * Firestore structure: applications/{applicationId} with userId field
     * 
     * @return Flow emitting a list of all applications
     */
    override fun getAllApplications(): Flow<List<Application>> = callbackFlow {
        val listener = firestore.collection("applications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val applications = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                    
                    val personalInfoData = data["personalInfo"] as? Map<String, Any>
                    val academicInfoData = data["academicInfo"] as? Map<String, Any>
                    val documentsData = data["documents"] as? List<Map<String, Any>>
                    
                    // Helper function to convert Firestore date to Date
                    fun convertToDate(value: Any?): Date? {
                        return when {
                            value is Date -> value
                            value != null && value.javaClass.name == "com.google.firebase.Timestamp" -> {
                                // Use reflection to call toDate() method
                                try {
                                    val toDateMethod = value.javaClass.getMethod("toDate")
                                    toDateMethod.invoke(value) as? Date
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            value != null && value.javaClass.name == "com.google.firebase.firestore.Timestamp" -> {
                                // Use reflection to call toDate() method
                                try {
                                    val toDateMethod = value.javaClass.getMethod("toDate")
                                    toDateMethod.invoke(value) as? Date
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            else -> null
                        }
                    }
                    
                        Application(
                            id = data["id"] as? String ?: doc.id,
                            userId = data["userId"] as? String ?: "",
                            personalInfo = PersonalInfo(
                                name = personalInfoData?.get("name") as? String ?: "",
                                dateOfBirth = convertToDate(personalInfoData?.get("dateOfBirth")),
                                idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                                email = personalInfoData?.get("email") as? String ?: "",
                                phone = personalInfoData?.get("phone") as? String ?: ""
                            ),
                            academicInfo = AcademicInfo(
                                academicDegree = academicInfoData?.get("academicDegree") as? String ?: "",
                                course = academicInfoData?.get("course") as? String ?: "",
                                studentNumber = academicInfoData?.get("studentNumber") as? String ?: "",
                                faesSupport = when (val faes = academicInfoData?.get("faesSupport")) {
                                    is Boolean -> faes
                                    is String -> faes == "Sim"
                                    else -> false
                                },
                                hasScholarship = when (val scholarship = academicInfoData?.get("hasScholarship")) {
                                    is Boolean -> scholarship
                                    is String -> scholarship == "Sim"
                                    else -> false
                                }
                            ),
                            documents = documentsData?.map { docData ->
                                ApplicationDocument(
                                    id = (docData["id"] as? Long)?.toInt() ?: 0,
                                    name = docData["name"] as? String ?: "",
                                    uri = null, // Stored as base64
                                    fileName = docData["fileName"] as? String,
                                    base64Data = docData["base64Data"] as? String // Store base64 for viewing
                                )
                            } ?: emptyList(),
                            submissionDate = when (val dateValue = data["submissionDate"]) {
                                is Date -> dateValue
                                else -> {
                                    // Try to convert Firestore Timestamp to Date using reflection
                                    if (dateValue != null) {
                                        val className = dateValue.javaClass.name
                                        if (className == "com.google.firebase.Timestamp" || className == "com.google.firebase.firestore.Timestamp") {
                                            try {
                                                val toDateMethod = dateValue.javaClass.getMethod("toDate")
                                                (toDateMethod.invoke(dateValue) as? Date) ?: Date()
                                            } catch (e: Exception) {
                                                Date()
                                            }
                                        } else {
                                            Date()
                                        }
                                    } else {
                                        Date()
                                    }
                                }
                            },
                            status = when (val statusValue = data["status"]) {
                                is Int -> ApplicationStatus.fromInt(statusValue)
                                is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                                is String -> ApplicationStatus.fromString(statusValue)
                                else -> ApplicationStatus.PENDING
                            }
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                trySend(applications)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Retrieves a specific application by its unique identifier.
     * 
     * This method fetches a single application document from Firestore
     * using the provided ID and converts it to the Application domain model.
     * It also verifies that the application belongs to the current user.
     * 
     * @param id The unique identifier of the application
     * @return Result containing the application if found, or error if not found
     */
    override suspend fun getApplicationById(id: String): Result<Application> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val doc = firestore.collection("applications")
                .document(id)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }

            val data = doc.data ?: return Result.failure(Exception("No data found"))
            
            // Verify that the application belongs to the current user
            val applicationUserId = data["userId"] as? String
            if (applicationUserId != userId) {
                return Result.failure(Exception("Unauthorized: Application does not belong to current user"))
            }
            
            val personalInfoData = data["personalInfo"] as? Map<String, Any>
            val academicInfoData = data["academicInfo"] as? Map<String, Any>
            val documentsData = data["documents"] as? List<Map<String, Any>>
            
            // Helper function to convert Firestore date to Date
            fun convertToDate(value: Any?): Date? {
                return when {
                    value is Date -> value
                    value != null && value.javaClass.name == "com.google.firebase.Timestamp" -> {
                        // Use reflection to call toDate() method
                        try {
                            val toDateMethod = value.javaClass.getMethod("toDate")
                            toDateMethod.invoke(value) as? Date
                        } catch (e: Exception) {
                            null
                        }
                    }
                    value != null && value.javaClass.name == "com.google.firebase.firestore.Timestamp" -> {
                        // Use reflection to call toDate() method
                        try {
                            val toDateMethod = value.javaClass.getMethod("toDate")
                            toDateMethod.invoke(value) as? Date
                        } catch (e: Exception) {
                            null
                        }
                    }
                    else -> null
                }
            }
            
            val application = Application(
                id = data["id"] as? String ?: doc.id,
                userId = applicationUserId ?: "",
                personalInfo = PersonalInfo(
                    name = personalInfoData?.get("name") as? String ?: "",
                    dateOfBirth = convertToDate(personalInfoData?.get("dateOfBirth")),
                    idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                    email = personalInfoData?.get("email") as? String ?: "",
                    phone = personalInfoData?.get("phone") as? String ?: ""
                ),
                academicInfo = AcademicInfo(
                    academicDegree = academicInfoData?.get("academicDegree") as? String ?: "",
                    course = academicInfoData?.get("course") as? String ?: "",
                    studentNumber = academicInfoData?.get("studentNumber") as? String ?: "",
                    faesSupport = when (val faes = academicInfoData?.get("faesSupport")) {
                        is Boolean -> faes
                        is String -> faes == "Sim"
                        else -> false
                    },
                    hasScholarship = when (val scholarship = academicInfoData?.get("hasScholarship")) {
                        is Boolean -> scholarship
                        is String -> scholarship == "Sim"
                        else -> false
                    }
                ),
                documents = documentsData?.map { docData ->
                    ApplicationDocument(
                        id = (docData["id"] as? Long)?.toInt() ?: 0,
                        name = docData["name"] as? String ?: "",
                        uri = null, // Stored as base64
                        fileName = docData["fileName"] as? String,
                        base64Data = docData["base64Data"] as? String
                    )
                } ?: emptyList(),
                submissionDate = when (val dateValue = data["submissionDate"]) {
                    is Date -> dateValue
                    else -> {
                        // Try to convert Firestore Timestamp to Date using reflection
                        if (dateValue != null) {
                            val className = dateValue.javaClass.name
                            if (className == "com.google.firebase.Timestamp" || className == "com.google.firebase.firestore.Timestamp") {
                                try {
                                    val toDateMethod = dateValue.javaClass.getMethod("toDate")
                                    (toDateMethod.invoke(dateValue) as? Date) ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                            } else {
                                Date()
                            }
                        } else {
                            Date()
                        }
                    }
                },
                status = when (val statusValue = data["status"]) {
                    is Int -> ApplicationStatus.fromInt(statusValue)
                    is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                    is String -> ApplicationStatus.fromString(statusValue)
                    else -> ApplicationStatus.PENDING
                },
                rejectionMessage = data["rejectionMessage"] as? String
            )

            Result.success(application)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a specific application by its unique identifier without user verification.
     * 
     * This method fetches a single application document from Firestore
     * using the provided ID and converts it to the Application domain model.
     * It does not verify that the application belongs to the current user,
     * allowing employees to view any application.
     * 
     * @param id The unique identifier of the application
     * @return Result containing the application if found, or error if not found
     */
    override suspend fun getApplicationByIdForEmployee(id: String): Result<Application> {
        return try {
            val doc = firestore.collection("applications")
                .document(id)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Application not found"))
            }

            val data = doc.data ?: return Result.failure(Exception("No data found"))
            
            val personalInfoData = data["personalInfo"] as? Map<String, Any>
            val academicInfoData = data["academicInfo"] as? Map<String, Any>
            val documentsData = data["documents"] as? List<Map<String, Any>>
            
            // Helper function to convert Firestore date to Date
            fun convertToDate(value: Any?): Date? {
                return when {
                    value is Date -> value
                    value != null && value.javaClass.name == "com.google.firebase.Timestamp" -> {
                        // Use reflection to call toDate() method
                        try {
                            val toDateMethod = value.javaClass.getMethod("toDate")
                            toDateMethod.invoke(value) as? Date
                        } catch (e: Exception) {
                            null
                        }
                    }
                    value != null && value.javaClass.name == "com.google.firebase.firestore.Timestamp" -> {
                        // Use reflection to call toDate() method
                        try {
                            val toDateMethod = value.javaClass.getMethod("toDate")
                            toDateMethod.invoke(value) as? Date
                        } catch (e: Exception) {
                            null
                        }
                    }
                    else -> null
                }
            }
            
            val applicationUserId = data["userId"] as? String
            
            val application = Application(
                id = data["id"] as? String ?: doc.id,
                userId = applicationUserId ?: "",
                personalInfo = PersonalInfo(
                    name = personalInfoData?.get("name") as? String ?: "",
                    dateOfBirth = convertToDate(personalInfoData?.get("dateOfBirth")),
                    idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                    email = personalInfoData?.get("email") as? String ?: "",
                    phone = personalInfoData?.get("phone") as? String ?: ""
                ),
                academicInfo = AcademicInfo(
                    academicDegree = academicInfoData?.get("academicDegree") as? String ?: "",
                    course = academicInfoData?.get("course") as? String ?: "",
                    studentNumber = academicInfoData?.get("studentNumber") as? String ?: "",
                    faesSupport = when (val faes = academicInfoData?.get("faesSupport")) {
                        is Boolean -> faes
                        is String -> faes == "Sim"
                        else -> false
                    },
                    hasScholarship = when (val scholarship = academicInfoData?.get("hasScholarship")) {
                        is Boolean -> scholarship
                        is String -> scholarship == "Sim"
                        else -> false
                    }
                ),
                documents = documentsData?.map { docData ->
                    ApplicationDocument(
                        id = (docData["id"] as? Long)?.toInt() ?: 0,
                        name = docData["name"] as? String ?: "",
                        uri = null, // Stored as base64
                        fileName = docData["fileName"] as? String,
                        base64Data = docData["base64Data"] as? String
                    )
                } ?: emptyList(),
                submissionDate = when (val dateValue = data["submissionDate"]) {
                    is Date -> dateValue
                    else -> {
                        // Try to convert Firestore Timestamp to Date using reflection
                        if (dateValue != null) {
                            val className = dateValue.javaClass.name
                            if (className == "com.google.firebase.Timestamp" || className == "com.google.firebase.firestore.Timestamp") {
                                try {
                                    val toDateMethod = dateValue.javaClass.getMethod("toDate")
                                    (toDateMethod.invoke(dateValue) as? Date) ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                            } else {
                                Date()
                            }
                        } else {
                            Date()
                        }
                    }
                },
                status = when (val statusValue = data["status"]) {
                    is Int -> ApplicationStatus.fromInt(statusValue)
                    is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                    is String -> ApplicationStatus.fromString(statusValue)
                    else -> ApplicationStatus.PENDING
                },
                rejectionMessage = data["rejectionMessage"] as? String
            )

            Result.success(application)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the status of an application.
     * 
     * This method allows employees/administrators to update the status of an application
     * (e.g., approve or reject). Optionally includes a rejection message when rejecting.
     * When approving an application, it also sets the user's isBeneficiary field to true.
     * 
     * @param applicationId The unique identifier of the application
     * @param status The new status to set
     * @param rejectionMessage Optional message to include when rejecting (null for approval)
     * @return Result indicating success or failure
     */
    override suspend fun updateApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
        rejectionMessage: String?
    ): Result<Unit> {
        return try {
            // First, get the application to retrieve the userId
            val applicationDoc = firestore.collection("applications")
                .document(applicationId)
                .get()
                .await()
            
            if (!applicationDoc.exists()) {
                return Result.failure(Exception("Application not found"))
            }
            
            val applicationData = applicationDoc.data ?: return Result.failure(Exception("No data found"))
            val userId = applicationData["userId"] as? String
                ?: return Result.failure(Exception("Application userId not found"))
            
            val updateData = mutableMapOf<String, Any>(
                "status" to status.value.toLong()
            )
            
            // Add rejection message if provided
            if (rejectionMessage != null && rejectionMessage.isNotBlank()) {
                updateData["rejectionMessage"] = rejectionMessage
            } else if (status != ApplicationStatus.REJECTED) {
                // Clear rejection message if not rejecting
                updateData["rejectionMessage"] = FieldValue.delete()
            }
            
            // Update application status
            firestore.collection("applications")
                .document(applicationId)
                .update(updateData)
                .await()
            
            // If approving, update user's isBeneficiary field to true
            if (status == ApplicationStatus.APPROVED) {
                firestore.collection("users")
                    .document(userId)
                    .update("isBeneficiary", true)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
