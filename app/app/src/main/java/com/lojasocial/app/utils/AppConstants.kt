package com.lojasocial.app.utils

/**
 * Application-wide constants.
 * 
 * This object contains shared constants used across the application,
 * including default values, URLs, and configuration settings.
 */
object AppConstants {
    /**
     * Base URL for the Loja Social API backend.
     * 
     * This is the main API endpoint for all backend services.
     */
    const val API_BASE_URL = "https://lojasocial-ipca.onrender.com/"
    
    /**
     * Default product image URL used as a fallback when a product doesn't have an image.
     * 
     * This URL points to a default placeholder image stored on Google Drive.
     * It is used throughout the app when displaying products that don't have
     * a specific image URL set.
     */
    const val DEFAULT_PRODUCT_IMAGE_URL = "https://drive.google.com/uc?export=view&id=1pFBQEmEMZOnUoDeQxus054ezCihRywPQ"
    
    /**
     * Default content description for product images when product name is not available.
     */
    const val DEFAULT_PRODUCT_CONTENT_DESCRIPTION = "Produto"
}
