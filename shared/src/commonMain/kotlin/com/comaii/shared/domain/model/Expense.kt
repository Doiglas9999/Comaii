package com.comaii.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    val companyId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: ExpenseType = ExpenseType.OTHER,
    val date: Long = 0L,
)

@Serializable
enum class ExpenseType {
    ENERGY, DELIVERY, EMPLOYEE, RENT, MARKETING, OTHER;

    fun label(): String = when (this) {
        ENERGY -> "Energia"
        DELIVERY -> "Motoboy/Entrega"
        EMPLOYEE -> "Funcionário"
        RENT -> "Aluguel"
        MARKETING -> "Marketing"
        OTHER -> "Outro"
    }
}
