package com.example.booking_service.validation

import com.example.booking_service.model.dto.CreateBookingRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class BookingTimeValidator : ConstraintValidator<ValidBookingTime, CreateBookingRequest> {

    override fun isValid(request: CreateBookingRequest?, context: ConstraintValidatorContext?): Boolean {
        if (request == null) {
            return true
        }

        return request.endTime.isAfter(request.startTime)
    }
}