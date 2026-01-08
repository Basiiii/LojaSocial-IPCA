package com.lojasocial.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Utility object for file operations in the Loja Social application.
 * 
 * This object provides utility methods for handling file operations
 * commonly needed in the application, particularly for document upload
 * and storage functionality. It includes Base64 conversion for Firestore
 * storage and filename extraction from URIs.
 * 
 * Key features:
 * - Convert files to Base64 for Firestore storage
 * - Extract filenames from file URIs
 * - Convert Base64 back to byte arrays
 * - Handle various URI schemes (content, file)
 * - Provide proper resource management with use blocks
 * 
 * @see Context Android context for ContentResolver access
 * @see Uri File URI for file operations
 * @see Base64 Android Base64 encoding/decoding utilities
 */
object FileUtils {
    
    /**
     * Converts a file URI to Base64 encoded string.
     * 
     * This method reads a file from the given URI and converts it to a
     * Base64 encoded string suitable for storage in Firestore documents.
     * It uses streaming to handle large files efficiently and provides
     * proper resource management.
     * 
     * Process:
     * 1. Opens InputStream from ContentResolver
     * 2. Reads file in chunks (1024 bytes)
     * 3. Converts to Base64 string
     * 4. Returns Result wrapper for error handling
     * 
     * @param context Android context for ContentResolver access
     * @param uri The URI of the file to convert
     * @return Result containing Base64 string if successful, or error if failed
     */
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
    
    /**
     * Extracts the filename from a file URI.
     * 
     * This method attempts to retrieve the display name of a file from its URI.
     * It first tries to get the filename from the ContentResolver using the
     * OpenableColumns.DISPLAY_NAME column. If that fails, it falls back to
     * extracting the filename from the URI path.
     * 
     * Process:
     * 1. Query ContentResolver for display name
     * 2. Extract filename from OpenableColumns.DISPLAY_NAME
     * 3. Fallback to URI path extraction
     * 4. Return "Ficheiro sem nome" as final fallback
     * 
     * @param context Android context for ContentResolver access
     * @param uri The URI of the file to extract the filename from
     * @return The filename if found, or "Ficheiro sem nome" as a fallback
     */
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
                result = result.substring(cut + 1)
            }
        }
        return result ?: "Ficheiro sem nome"
    }
    
    /**
     * Converts a Base64 encoded string back to a byte array.
     * 
     * This method decodes a Base64 string back to its original byte array
     * representation. This is useful when retrieving files from Firestore
     * storage where they were stored as Base64 strings.
     * 
     * Use cases:
     * - Retrieving files from Firestore documents
     * - Converting stored Base64 data back to file format
     * - Preparing data for file recreation or download
     * 
     * @param base64 The Base64 encoded string to decode
     * @return Result containing byte array if successful, or error if failed
     */
    fun convertBase64ToFile(base64: String): Result<ByteArray> {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Compresses and resizes an image from a URI to a byte array.
     * 
     * This method reads an image from the given URI, resizes it to fit within
     * the specified maximum dimensions while maintaining aspect ratio, and
     * compresses it as a JPEG with the specified quality. This is useful for
     * reducing image file sizes before storing them as Base64 strings in Firestore.
     * 
     * Process:
     * 1. Decodes the image from URI using BitmapFactory
     * 2. Calculates scaling factor to fit within maxWidth x maxHeight
     * 3. Resizes the bitmap while maintaining aspect ratio
     * 4. Compresses to JPEG format with specified quality
     * 5. Returns compressed byte array
     * 
     * @param context Android context for ContentResolver access
     * @param uri The URI of the image to compress
     * @param maxWidth Maximum width in pixels (default: 800)
     * @param maxHeight Maximum height in pixels (default: 800)
     * @param quality Compression quality (0-100, default: 85)
     * @return Result containing compressed byte array if successful, or error if failed
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 85
    ): Result<ByteArray> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // Decode with options to get dimensions without loading full image
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calculate scaling factor
                val scale = minOf(
                    maxWidth.toFloat() / options.outWidth,
                    maxHeight.toFloat() / options.outHeight
                ).coerceAtMost(1.0f)
                
                // Reset stream position by creating new input stream
                val resizedStream = context.contentResolver.openInputStream(uri)
                resizedStream?.use { resized ->
                    // Decode with scaling
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = if (scale < 1.0f) {
                            (1.0f / scale).toInt().coerceAtLeast(1)
                        } else {
                            1
                        }
                    }
                    
                    var bitmap = BitmapFactory.decodeStream(resized, null, decodeOptions)
                        ?: return Result.failure(Exception("Failed to decode image"))
                    
                    // Further resize if needed to ensure exact dimensions
                    if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                        val width = bitmap.width
                        val height = bitmap.height
                        val ratio = minOf(
                            maxWidth.toFloat() / width,
                            maxHeight.toFloat() / height
                        )
                        val newWidth = (width * ratio).toInt()
                        val newHeight = (height * ratio).toInt()
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    }
                    
                    // Compress to JPEG
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    bitmap.recycle()
                    
                    Result.success(outputStream.toByteArray())
                } ?: Result.failure(Exception("Failed to open image stream"))
            } ?: Result.failure(Exception("Failed to open input stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Converts an image URI to Base64 string with compression.
     * 
     * This is a convenience method that combines image compression and Base64 encoding.
     * It compresses the image first, then converts it to a Base64 string suitable for
     * storage in Firestore.
     * 
     * @param context Android context for ContentResolver access
     * @param uri The URI of the image to convert
     * @param maxWidth Maximum width in pixels (default: 800)
     * @param maxHeight Maximum height in pixels (default: 800)
     * @param quality Compression quality (0-100, default: 85)
     * @return Result containing Base64 string if successful, or error if failed
     */
    fun convertImageToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 85
    ): Result<String> {
        return compressImage(context, uri, maxWidth, maxHeight, quality)
            .mapCatching { bytes ->
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
    }
}
