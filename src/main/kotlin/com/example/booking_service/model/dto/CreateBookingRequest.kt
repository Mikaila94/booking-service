package com.example.booking_service.model.dto

import com.example.booking_service.validation.ValidBookingTime
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

@ValidBookingTime
data class CreateBookingRequest(
    @NotNull(message = "User ID is required")
    val userId: UUID,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    val startTime: Instant,

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    val endTime: Instant
)

