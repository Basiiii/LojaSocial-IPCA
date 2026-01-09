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
    object PickupRequests : Screen("pickupRequests")
    
    // Application Flow
    object ApplicationFlow : Screen("applicationFlow") {
        object PersonalInfo : Screen("applicationFlow/personalInfo")
        object AcademicData : Screen("applicationFlow/academicData")
        object Documents : Screen("applicationFlow/documents")
    }
    
    // Applications
    object ApplicationsList : Screen("applicationsList")
    object AllApplicationsList : Screen("allApplicationsList") // For employees to review all applications
    data class ApplicationDetail(val applicationId: String = "{applicationId}") : Screen("applicationDetail/{applicationId}") {
        companion object {
            fun createRoute(applicationId: String) = "applicationDetail/$applicationId"
        }
    }
    
    // Stock
    object ExpiringItems : Screen("expiringItems")
    object StockList : Screen("stockList")
    data class StockItems(val barcode: String = "{barcode}") : Screen("stockItems/{barcode}") {
        companion object {
            fun createRoute(barcode: String) = "stockItems/$barcode"
        }
    }
    
    // Activity
    object ActivityList : Screen("activityList")
    
    // Campaigns
    object CampaignsList : Screen("campaignsList")
    object CreateCampaign : Screen("createCampaign") {
        data class Edit(val campaignId: String = "{campaignId}") : Screen("createCampaign/{campaignId}") {
            companion object {
                fun createRoute(campaignId: String) = "createCampaign/$campaignId"
            }
        }
    }
    data class CampaignProducts(val campaignId: String = "{campaignId}") : Screen("campaignProducts/{campaignId}") {
        companion object {
            fun createRoute(campaignId: String) = "campaignProducts/$campaignId"
        }
    }
    
    // Audit Logs
    object AuditLogs : Screen("auditLogs")
    
    // Beneficiaries
    object BeneficiariesList : Screen("beneficiariesList")
    data class BeneficiaryDetail(val userId: String = "{userId}") : Screen("beneficiaryDetail/{userId}") {
        companion object {
            fun createRoute(userId: String) = "beneficiaryDetail/$userId"
        }
    }
}
