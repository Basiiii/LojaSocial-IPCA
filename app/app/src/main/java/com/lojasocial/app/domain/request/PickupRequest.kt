package com.lojasocial.app.domain.request

import java.util.Date

/**
 * Represents a pickup request that was completed on a specific date.
 */
data class PickupRequest(
    val id: String,
    val userId: String,
    val userName: String?,
    val totalItems: Int,
    val pickupDate: Date
)
