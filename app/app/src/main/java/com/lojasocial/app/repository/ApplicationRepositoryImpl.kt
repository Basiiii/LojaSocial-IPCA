package com.lojasocial.app.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.Application
import com.lojasocial.app.domain.ApplicationDocument
import com.lojasocial.app.domain.ApplicationStatus
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ApplicationRepository {

    private var context: Context? = null
    
    override fun setContext(context: Context) {
        this.context = context
    }

    override suspend fun submitApplication(application: Application): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Create application document in users/{userId}/applications subcollection
            val applicationsRef = firestore.collection("users")
                .document(userId)
                .collection("applications")
            
            val applicationWithId = application.copy(id = applicationsRef.document().id)
            
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
                "status" to application.status.name,
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

    override suspend fun convertFileToBase64(uri: Uri): Result<String> {
        return try {
            context?.let { ctx ->
                FileUtils.convertFileToBase64(ctx, uri)
            } ?: Result.failure(Exception("Context not set"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getApplications(): Flow<List<Application>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("applications")
                .get()
                .await()

            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val personalInfoData = data["personalInfo"] as? Map<String, Any>
                    val academicInfoData = data["academicInfo"] as? Map<String, Any>
                    val documentsData = data["documents"] as? List<Map<String, Any>>
                    
                    Application(
                        id = data["id"] as? String ?: doc.id,
                        personalInfo = com.lojasocial.app.domain.PersonalInfo(
                            name = personalInfoData?.get("name") as? String ?: "",
                            dateOfBirth = personalInfoData?.get("dateOfBirth") as? Date,
                            idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                            email = personalInfoData?.get("email") as? String ?: "",
                            phone = personalInfoData?.get("phone") as? String ?: ""
                        ),
                        academicInfo = com.lojasocial.app.domain.AcademicInfo(
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
                                uri = null, // We'll store base64 data instead
                                fileName = docData["fileName"] as? String
                            )
                        } ?: emptyList(),
                        submissionDate = data["submissionDate"] as? Date ?: Date(),
                        status = try {
                            ApplicationStatus.valueOf(data["status"] as? String ?: ApplicationStatus.PENDING.name)
                        } catch (e: Exception) {
                            ApplicationStatus.PENDING
                        }
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

    override suspend fun getApplicationById(id: String): Result<Application> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val doc = firestore.collection("users")
                .document(userId)
                .collection("applications")
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
            
            val application = Application(
                id = data["id"] as? String ?: doc.id,
                personalInfo = com.lojasocial.app.domain.PersonalInfo(
                    name = personalInfoData?.get("name") as? String ?: "",
                    dateOfBirth = personalInfoData?.get("dateOfBirth") as? Date,
                    idPassport = personalInfoData?.get("idPassport") as? String ?: "",
                    email = personalInfoData?.get("email") as? String ?: "",
                    phone = personalInfoData?.get("phone") as? String ?: ""
                ),
                academicInfo = com.lojasocial.app.domain.AcademicInfo(
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
                        uri = null, // We'll store base64 data instead
                        fileName = docData["fileName"] as? String
                    )
                } ?: emptyList(),
                submissionDate = data["submissionDate"] as? Date ?: Date(),
                status = try {
                    ApplicationStatus.valueOf(data["status"] as? String ?: ApplicationStatus.PENDING.name)
                } catch (e: Exception) {
                    ApplicationStatus.PENDING
                }
            )

            Result.success(application)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
