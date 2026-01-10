import express from 'express';
import axios from 'axios';
import FormData from 'form-data';
import logger from '../utils/logger.js';
import { validateAuth } from '../middleware/auth.js';

const router = express.Router();

/**
 * POST /api/image/remove-background
 * Removes the background from an image using remove.bg API
 * 
 * Request body:
 * {
 *   "imageBase64": "base64-encoded-image-string"
 * }
 * 
 * Response:
 * {
 *   "success": true,
 *   "imageBase64": "base64-encoded-image-without-background"
 * }
 */
router.post('/remove-background', async (req, res) => {
  logger.server('Remove background request received');
  
  if (!validateAuth(req)) {
    logger.server('Unauthorized request');
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const apiKey = process.env.REMOVE_BG_API_KEY;
  
  if (!apiKey) {
    logger.server('Remove.bg API key not configured');
    return res.status(500).json({ error: 'Remove.bg API key not configured' });
  }

  try {
    const { imageBase64 } = req.body;

    if (!imageBase64) {
      logger.server('Missing imageBase64 parameter');
      return res.status(400).json({ error: 'imageBase64 parameter is required' });
    }

    // Validate base64 string
    if (typeof imageBase64 !== 'string' || imageBase64.trim().length === 0) {
      logger.server('Invalid imageBase64 format');
      return res.status(400).json({ error: 'Invalid imageBase64 format' });
    }

    // Remove data URI prefix if present
    let cleanBase64 = imageBase64.trim();
    if (cleanBase64.startsWith('data:image')) {
      cleanBase64 = cleanBase64.split(',')[1];
    }

    // Convert base64 to buffer
    let imageBuffer;
    try {
      imageBuffer = Buffer.from(cleanBase64, 'base64');
    } catch (e) {
      logger.server('Failed to decode base64 image');
      return res.status(400).json({ error: 'Invalid base64 encoding' });
    }

    // Validate image buffer size (minimum 1KB, maximum 10MB)
    if (imageBuffer.length < 1024) {
      logger.server('Image too small');
      return res.status(400).json({ error: 'Image is too small (minimum 1KB)' });
    }
    if (imageBuffer.length > 10 * 1024 * 1024) {
      logger.server('Image too large');
      return res.status(400).json({ error: 'Image is too large (maximum 10MB)' });
    }

    // Detect image format from buffer
    let contentType = 'image/jpeg';
    let filename = 'product.jpg';
    if (imageBuffer[0] === 0x89 && imageBuffer[1] === 0x50) {
      // PNG signature
      contentType = 'image/png';
      filename = 'product.png';
    }

    // Create form data for remove.bg API
    const formData = new FormData();
    formData.append('image_file', imageBuffer, {
      filename: filename,
      contentType: contentType
    });
    formData.append('size', 'regular'); // Options: regular, medium, hd, 4k
    // Don't specify ROI - let remove.bg auto-detect the subject
    // formData.append('roi', '0% 0% 100% 100%'); // Only if needed

    logger.server(`Sending request to remove.bg API (${(imageBuffer.length / 1024).toFixed(2)}KB, ${contentType})`);
    const response = await axios.post('https://api.remove.bg/v1.0/removebg', formData, {
      headers: {
        'X-Api-Key': apiKey,
        ...formData.getHeaders()
      },
      responseType: 'arraybuffer', // Get binary response
      maxContentLength: 20 * 1024 * 1024, // 20MB max response
      timeout: 30000 // 30 second timeout
    });

    // Convert response to base64
    const processedImageBase64 = Buffer.from(response.data, 'binary').toString('base64');

    logger.server('Background removed successfully');
    res.json({
      success: true,
      imageBase64: processedImageBase64
    });
  } catch (error) {
    logger.error('Remove.bg API error', error);
    
    let errorMessage = error.message;
    let statusCode = error.response?.status || 500;
    
    // Parse remove.bg API error response
    if (error.response?.data) {
      try {
        const errorData = Buffer.from(error.response.data).toString('utf-8');
        const parsedError = JSON.parse(errorData);
        
        if (parsedError.errors && parsedError.errors.length > 0) {
          const firstError = parsedError.errors[0];
          errorMessage = firstError.detail || firstError.title || errorMessage;
          
          // Provide helpful messages for common errors
          if (firstError.code === 'roi_region_empty') {
            errorMessage = 'Unable to detect subject in image. Please ensure the image contains a clear product/subject with a distinguishable background.';
            statusCode = 400;
          } else if (firstError.code === 'image_too_small') {
            errorMessage = 'Image is too small. Minimum size is 100x100 pixels.';
            statusCode = 400;
          } else if (firstError.code === 'image_too_large') {
            errorMessage = 'Image is too large. Maximum size is 25 megapixels.';
            statusCode = 400;
          } else if (firstError.code === 'invalid_image_format') {
            errorMessage = 'Invalid image format. Supported formats: JPG, PNG, WEBP.';
            statusCode = 400;
          }
        } else {
          errorMessage = parsedError.error?.message || errorData;
        }
      } catch (parseError) {
        // If we can't parse the error, use the raw response
        errorMessage = Buffer.from(error.response.data).toString('utf-8');
      }
    }
    
    res.status(statusCode).json({
      error: 'Failed to remove background',
      details: errorMessage
    });
  }
});

export default router;
