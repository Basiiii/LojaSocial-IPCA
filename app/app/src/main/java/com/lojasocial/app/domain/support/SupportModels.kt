package com.lojasocial.app.domain.support

/**
 * Domain model representing a frequently asked question.
 * 
 * This entity represents an FAQ item in the domain layer, containing
 * the question and answer content independent of UI concerns.
 * 
 * @property question The FAQ question text.
 * @property answer The FAQ answer text.
 */
data class FaqItem(
    val question: String,
    val answer: String
)

/**
 * Domain repository for FAQ data.
 * 
 * This object provides access to frequently asked questions data.
 * In a real application, this would fetch data from a repository,
 * database, or API. For now, it contains sample data.
 */
object FaqRepository {
    /**
     * Sample FAQ data for the support section.
     * 
     * This collection contains common questions and answers about the Loja Social
     * application, including information about AI functionality, support hours,
     * and data management.
     * 
     * @return List of FAQ items for display in the support section.
     */
    fun getFaqItems(): List<FaqItem> = listOf(
        FaqItem(
            question = "Como funciona a IA?",
            answer = "A nossa inteligência artificial processa os seus dados de forma segura para lhe dar respostas rápidas e precisas baseadas no histórico da conversa."
        ),
        FaqItem(
            question = "O suporte funciona 24 horas?",
            answer = "O nosso assistente virtual (LLM) está disponível 24/7. O suporte humano funciona das 9h às 18h nos dias úteis."
        ),
        FaqItem(
            question = "Como posso contactar o suporte?",
            answer = "Pode contactar-nos através do chat disponível na aplicação ou por email para suporte@lojasocial.pt."
        ),
    )
}
