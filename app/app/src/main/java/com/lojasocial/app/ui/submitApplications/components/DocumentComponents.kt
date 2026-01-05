package com.lojasocial.app.ui.submitApplications.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.PurpleIcon
import com.lojasocial.app.ui.theme.PurpleLight
import com.lojasocial.app.ui.theme.RedDelete

/**
 * UI representation of a document in the application form.
 * 
 * This data class represents a document that can be uploaded as part
 * of the scholarship application. It contains the document's display name
 * and the URI of the selected file (null if not yet uploaded).
 * 
 * @property id Unique identifier for the document within the application
 * @property name Display name of the document (e.g., "Comprovativo de Inscrição no IPCA")
 * @property uri URI of the selected file (null if no file has been selected)
 */
data class DocumentUi(
    val id: Int,
    val name: String,
    val uri: Uri? = null
)

/**
 * Card component for document upload in the application form.
 * 
 * This composable provides a card interface for uploading and managing
 * documents required for the scholarship application. It handles file selection,
 * display of uploaded files, and deletion of uploaded files.
 * 
 * Features:
 * - Shows document name and upload status
 * - Allows file selection through system file picker
 * - Displays uploaded file name when available
 * - Provides delete functionality for uploaded files
 * - Uses consistent styling with the application theme
 * 
 * @param document The document data to display and manage
 * @param onDelete Callback invoked when the user wants to delete the uploaded file
 * @param onUpload Callback invoked with the selected file URI when a file is chosen
 */
@Composable
fun DocumentUploadCard(
    document: DocumentUi,
    onDelete: () -> Unit,
    onUpload: (Uri) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = PurpleIcon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.Black
                    )
                    if (document.uri != null) {
                        Text(
                            text = getFileName(context, document.uri),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "Nenhum ficheiro selecionado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Row {
                if (document.uri == null) {
                    FileUploadButton(
                        onFileSelected = onUpload
                    )
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover ficheiro",
                            tint = RedDelete,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * File upload button component for document selection.
 * 
 * This composable provides a button that opens the system file picker
 * to allow users to select PDF files for upload. It uses the ActivityResultContracts
 * to handle file selection and provides visual feedback with a purple-themed button.
 * 
 * Features:
 * - Opens system file picker filtered for PDF files
 * - Provides visual feedback with purple styling
 * - Handles file selection through callback
 * - Uses rounded corners and consistent theming
 * 
 * @param onFileSelected Callback invoked when a file is selected, receives the file URI
 */
@Composable
fun FileUploadButton(
    onFileSelected: (Uri) -> Unit
) {
    /**
     * Activity result launcher for file selection.
     * 
     * This launcher opens the system file picker and filters for PDF files.
     * When a file is selected, it invokes the onFileSelected callback with the URI.
     */
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }
    
    /**
     * Upload button with purple styling and add icon.
     */
    IconButton(
        onClick = { launcher.launch("application/pdf") },
        modifier = Modifier
            .size(40.dp)
            .background(PurpleLight, shape = RoundedCornerShape(8.dp))
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Adicionar ficheiro",
            tint = PurpleIcon,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Button component for adding additional documents.
 * 
 * This composable provides an outlined button for adding new documents
 * to the application form. It features a consistent design with the app theme
 * and includes an add icon with descriptive text.
 * 
 * Features:
 * - Outlined button style with primary color border
 * - Add icon and descriptive text
 * - Consistent sizing and theming
 * - Full width layout for better accessibility
 * 
 * @param onAddDocument Callback invoked when the button is clicked
 */
@Composable
fun AddDocumentButton(
    onAddDocument: () -> Unit
) {
    OutlinedButton(
        onClick = onAddDocument,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LojaSocialPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LojaSocialPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Adicionar documento",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Adicionar Documento",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Utility function to extract the filename from a URI.
 * 
 * This function retrieves the display name of a file from its URI using
 * the ContentResolver and OpenableColumns. It provides a fallback
 * filename if the display name cannot be retrieved.
 * 
 * @param context Android context for accessing ContentResolver
 * @param uri The URI of the file to extract the filename from
 * @return The filename if found, or "unknown" as a fallback
 */
fun getFileName(context: Context, uri: Uri): String {
    var fileName = "unknown"
    uri.let { returnUri ->
        context.contentResolver.query(returnUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex) ?: "unknown"
        }
    }
    return fileName
}
