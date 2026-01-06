package com.lojasocial.app.repository

import com.lojasocial.app.domain.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing recent activity data.
 * 
 * This interface defines methods for retrieving recent activities for both
 * beneficiaries and employees, aggregating data from requests and applications.
 */
interface ActivityRepository {
    /**
     * Retrieves recent activities for a beneficiary user.
     * 
     * Returns activities related to the current user's requests, ordered by
     * most recent first.
     * 
     * @param limit Maximum number of activities to return
     * @return Flow emitting lists of activities
     */
    fun getRecentActivitiesForBeneficiary(limit: Int = 20): Flow<List<Activity>>
    
    /**
     * Retrieves recent activities for an employee/admin user.
     * 
     * Returns activities from all users including requests and applications,
     * ordered by most recent first.
     * 
     * @param limit Maximum number of activities to return
     * @return Flow emitting lists of activities
     */
    fun getRecentActivitiesForEmployee(limit: Int = 20): Flow<List<Activity>>
}
