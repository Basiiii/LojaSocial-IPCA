package com.lojasocial.app.navigation

/**
 * Sealed class hierarchy for type-safe navigation routes.
 * This provides compile-time safety and better organization of navigation destinations.
 */
sealed class Screen(val route: String) {
    // Authentication
    object Login : Screen("login")
    
    // Portal Selection
    object PortalSelection : Screen("portalSelection")
    
    // Main Portals (with nested tab navigation)
    object EmployeePortal : Screen("employeePortal") {
        object Home : Screen("employeePortal/home")
        object Profile : Screen("employeePortal/profile")
        object Support : Screen("employeePortal/support")
        object Calendar : Screen("employeePortal/calendar")
    }
    
    object BeneficiaryPortal : Screen("beneficiaryPortal") {
        object Home : Screen("beneficiaryPortal/home")
        object Profile : Screen("beneficiaryPortal/profile")
        object Support : Screen("beneficiaryPortal/support")
        object Calendar : Screen("beneficiaryPortal/calendar")
    }
    
    object NonBeneficiaryPortal : Screen("nonBeneficiaryPortal") {
        object Home : Screen("nonBeneficiaryPortal/home")
        object Profile : Screen("nonBeneficiaryPortal/profile")
        object Support : Screen("nonBeneficiaryPortal/support")
        object Calendar : Screen("nonBeneficiaryPortal/calendar")
    }
    
    // Feature Screens
    object RequestItems : Screen("requestItems")
    
    // Application Flow
    object ApplicationFlow : Screen("applicationFlow") {
        object PersonalInfo : Screen("applicationFlow/personalInfo")
        object AcademicData : Screen("applicationFlow/academicData")
        object Documents : Screen("applicationFlow/documents")
    }
    
    // Applications
    object ApplicationsList : Screen("applicationsList")
    data class ApplicationDetail(val applicationId: String = "{applicationId}") : Screen("applicationDetail/{applicationId}") {
        companion object {
            fun createRoute(applicationId: String) = "applicationDetail/$applicationId"
        }
    }
}
