package com.lojasocial.app.ui.applications.components

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
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.PurpleIcon
import com.lojasocial.app.ui.theme.PurpleLight
import com.lojasocial.app.ui.theme.RedDelete

data class DocumentoUi(
    val id: Int,
    val nome: String,
    val uri: Uri? = null
)

@Composable
fun DocumentUploadCard(
    document: DocumentoUi,
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
                        text = document.nome,
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
                            contentDescription = "Eliminar",
                            tint = RedDelete,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileUploadButton(
    onFileSelected: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }
    
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
