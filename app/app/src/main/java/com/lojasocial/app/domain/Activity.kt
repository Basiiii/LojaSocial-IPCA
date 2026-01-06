package com.lojasocial.app.domain

import java.util.Date

/**
 * Represents a recent activity in the system.
 * 
 * Activities are derived from existing entities (requests, applications) and
 * displayed in the recent activity feed for both beneficiaries and employees.
 * 
 * @property id Unique identifier for the activity (usually the entity ID)
 * @property type The type of activity that occurred
 * @property title Localized title for the activity
 * @property subtitle Additional context or description
 * @property timestamp When the activity occurred
 * @property userId The user ID associated with the activity (for employee view)
 * @property userName The user name associated with the activity (for employee view)
 */
data class Activity(
    val id: String,
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: Date,
    val userId: String? = null,
    val userName: String? = null
)

/**
 * Enumeration of activity types in the system.
 * 
 * These represent different events that can appear in the recent activity feed.
 */
enum class ActivityType {
    /** A request was submitted by a beneficiary */
    REQUEST_SUBMITTED,
    
    /** A request was accepted by an employee */
    REQUEST_ACCEPTED,
    
    /** A pickup/collection was completed */
    PICKUP_COMPLETED,
    
    /** A new application was submitted */
    APPLICATION_SUBMITTED,
    
    /** An application was approved */
    APPLICATION_APPROVED
}
