package com.comaii.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val categoryId: String = "",
    val isAvailable: Boolean = true,
    val order: Int = 0,
)

@Serializable
data class Category(
    val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val order: Int = 0,
)
