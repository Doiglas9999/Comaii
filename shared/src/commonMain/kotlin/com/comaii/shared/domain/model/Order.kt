package com.comaii.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String = "",
    val companyId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val notes: String = "",
    val createdAt: Long = 0L,
)

@Serializable
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val notes: String = "",
) {
    val subtotal: Double get() = quantity * unitPrice
}

@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DELIVERING,
    DELIVERED,
    CANCELLED,
}
