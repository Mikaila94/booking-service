package com.example.booking_service.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BookingTimeValidator::class])
annotation class ValidBookingTime(
    val message: String = "End time must be after start time",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)