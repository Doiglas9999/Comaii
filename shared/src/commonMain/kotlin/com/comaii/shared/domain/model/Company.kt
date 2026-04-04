package com.comaii.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val primaryColor: String = "#FF6B00",
    val secondaryColor: String = "#FFFFFF",
    val accentColor: String = "#333333",
    val fontFamily: String = "default",
    val phone: String = "",
    val address: String = "",
    val isOpen: Boolean = true,
    val ownerId: String = "",
    val createdAt: Long = 0L,
)
