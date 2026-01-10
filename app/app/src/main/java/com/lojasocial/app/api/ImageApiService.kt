package com.lojasocial.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the Image API service.
 * 
 * This interface defines the HTTP endpoints for communicating with the
 * Loja Social image processing API hosted at https://lojasocial-ipca.onrender.com/
 * 
 * The API uses POST requests for image processing with Bearer token authentication.
 */
interface ImageApiService {
    /**
     * Removes the background from an image using remove.bg API.
     * 
     * @param authorization The Bearer token for API authentication (format: "Bearer lojasocial2025")
     * @param contentType The content type of the request (should be "application/json")
     * @param request The image processing request containing the base64-encoded image
     * @return Response containing the processed image as base64 string
     */
    @POST("api/image/remove-background")
    suspend fun removeBackground(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String,
        @Body request: RemoveBackgroundRequest
    ): Response<RemoveBackgroundResponse>
}

/**
 * Request model for remove background API.
 */
data class RemoveBackgroundRequest(
    val imageBase64: String
)

/**
 * Response model for remove background API.
 */
data class RemoveBackgroundResponse(
    val success: Boolean,
    val imageBase64: String
)
