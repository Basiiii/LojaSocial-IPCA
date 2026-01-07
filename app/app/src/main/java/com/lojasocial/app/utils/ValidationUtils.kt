package com.lojasocial.app.utils

import java.util.regex.Pattern

/**
 * Utility class for form validation.
 * Provides validation functions for common input types.
 */
object ValidationUtils {
    
    /**
     * Email validation pattern
     */
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )
    
    /**
     * Portuguese phone number pattern (9 digits, may start with +351)
     */
    private val PHONE_PATTERN = Pattern.compile(
        "^(\\+351)?[0-9]{9}\$"
    )
    
    /**
     * Portuguese ID/Passport pattern (alphanumeric, 6-12 characters)
     */
    private val ID_PASSPORT_PATTERN = Pattern.compile(
        "^[A-Za-z0-9]{6,12}\$"
    )
    
    /**
     * Student number pattern (alphanumeric, 5-10 characters)
     */
    private val STUDENT_NUMBER_PATTERN = Pattern.compile(
        "^[A-Za-z0-9]{5,10}\$"
    )
    
    /**
     * Validates an email address.
     * 
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_PATTERN.matcher(email.trim()).matches()
    }
    
    /**
     * Validates a Portuguese phone number.
     * 
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhone(phone: String): Boolean {
        // Remove spaces and dashes for validation
        val cleanedPhone = phone.replace("\\s|-".toRegex(), "")
        return cleanedPhone.isNotBlank() && PHONE_PATTERN.matcher(cleanedPhone).matches()
    }
    
    /**
     * Validates an ID/Passport number.
     * 
     * @param idPassport The ID/Passport to validate
     * @return true if valid, false otherwise
     */
    fun isValidIdPassport(idPassport: String): Boolean {
        return idPassport.isNotBlank() && ID_PASSPORT_PATTERN.matcher(idPassport.trim()).matches()
    }
    
    /**
     * Validates a student number.
     * 
     * @param studentNumber The student number to validate
     * @return true if valid, false otherwise
     */
    fun isValidStudentNumber(studentNumber: String): Boolean {
        return studentNumber.isNotBlank() && STUDENT_NUMBER_PATTERN.matcher(studentNumber.trim()).matches()
    }
    
    /**
     * Validates a name (must be at least 2 characters, only letters and spaces).
     * 
     * @param name The name to validate
     * @return true if valid, false otherwise
     */
    fun isValidName(name: String): Boolean {
        val namePattern = Pattern.compile("^[A-Za-zÀ-ÿ\\s]{2,50}\$")
        return name.isNotBlank() && namePattern.matcher(name.trim()).matches()
    }
    
    /**
     * Gets validation error message for email.
     */
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email é obrigatório"
            !isValidEmail(email) -> "Email inválido. Exemplo: exemplo@email.com"
            else -> null
        }
    }
    
    /**
     * Gets validation error message for phone.
     */
    fun getPhoneError(phone: String): String? {
        return when {
            phone.isBlank() -> "Telemóvel é obrigatório"
            !isValidPhone(phone) -> "Telemóvel inválido. Use 9 dígitos (ex: 912345678)"
            else -> null
        }
    }
    
    /**
     * Gets validation error message for ID/Passport.
     */
    fun getIdPassportError(idPassport: String): String? {
        return when {
            idPassport.isBlank() -> "CC/Passaporte é obrigatório"
            !isValidIdPassport(idPassport) -> "CC/Passaporte inválido. Use 6-12 caracteres alfanuméricos"
            else -> null
        }
    }
    
    /**
     * Gets validation error message for student number.
     */
    fun getStudentNumberError(studentNumber: String): String? {
        return when {
            studentNumber.isBlank() -> "Número de estudante é obrigatório"
            !isValidStudentNumber(studentNumber) -> "Número de estudante inválido. Use 5-10 caracteres alfanuméricos"
            else -> null
        }
    }
    
    /**
     * Gets validation error message for name.
     */
    fun getNameError(name: String): String? {
        return when {
            name.isBlank() -> "Nome é obrigatório"
            !isValidName(name) -> "Nome inválido. Use apenas letras e espaços (2-50 caracteres)"
            else -> null
        }
    }
}

