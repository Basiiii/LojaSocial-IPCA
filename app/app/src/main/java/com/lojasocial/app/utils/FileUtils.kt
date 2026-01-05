package com.lojasocial.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FileUtils {
    
    fun convertFileToBase64(context: Context, uri: Uri): Result<String> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            
            inputStream?.use { input ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, length)
                }
            }
            
            val bytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            Result.success(base64String)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Ficheiro sem nome"
    }
    
    fun convertBase64ToFile(base64: String): Result<ByteArray> {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
