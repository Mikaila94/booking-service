package com.example.booking_service.model.dto

import java.time.Instant
import java.util.UUID

data class BookingDTO(
    val id: UUID,
    val userId: UUID,
    val startTime: Instant,
    val endTime: Instant,
    val status: String,
    val isExpired: Boolean
)