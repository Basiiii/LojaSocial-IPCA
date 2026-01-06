package com.lojasocial.app.navigation

import com.lojasocial.app.repository.UserProfile

/**
 * Helper functions for navigation logic.
 */
object NavigationHelper {
    /**
     * Determines the appropriate start destination based on user profile.
     * 
     * @param userProfile The current user's profile, or null if not logged in
     * @return The route string for the start destination
     */
    fun getDestinationForUser(userProfile: UserProfile?): String {
        return when {
            userProfile == null -> Screen.Login.route
            userProfile.isAdmin && userProfile.isBeneficiary -> Screen.PortalSelection.route
            !userProfile.isAdmin && !userProfile.isBeneficiary -> Screen.NonBeneficiaryPortal.route
            userProfile.isAdmin -> Screen.EmployeePortal.route
            else -> Screen.BeneficiaryPortal.route
        }
    }
    
    /**
     * Gets the default tab route for a portal.
     */
    fun getDefaultTabRoute(portalRoute: String): String {
        return when (portalRoute) {
            Screen.EmployeePortal.route -> Screen.EmployeePortal.Home.route
            Screen.BeneficiaryPortal.route -> Screen.BeneficiaryPortal.Home.route
            Screen.NonBeneficiaryPortal.route -> Screen.NonBeneficiaryPortal.Home.route
            else -> portalRoute
        }
    }
    
    /**
     * Extracts the tab name from a portal tab route.
     */
    fun getTabFromRoute(route: String): String {
        return route.substringAfterLast("/")
    }
}
