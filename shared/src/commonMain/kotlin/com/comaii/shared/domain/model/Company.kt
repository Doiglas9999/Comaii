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
    val workingDays: WorkingDays = WorkingDays(),
)

@Serializable
data class WorkingDays(
    val monday: DaySchedule = DaySchedule(),
    val tuesday: DaySchedule = DaySchedule(),
    val wednesday: DaySchedule = DaySchedule(),
    val thursday: DaySchedule = DaySchedule(),
    val friday: DaySchedule = DaySchedule(),
    val saturday: DaySchedule = DaySchedule(),
    val sunday: DaySchedule = DaySchedule(),
)

@Serializable
data class DaySchedule(
    val isOpen: Boolean = false,
    val openTime: String = "08:00",
    val closeTime: String = "18:00",
)
