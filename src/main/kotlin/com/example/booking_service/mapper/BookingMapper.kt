package com.example.booking_service.mapper

import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.model.entity.Booking

object BookingMapper {
    fun toDTO(booking: Booking): BookingDTO {
        return BookingDTO(
            id = booking.id!!,
            userId = booking.userId!!,
            startTime = booking.startTime!!,
            endTime = booking.endTime!!,
            status = booking.status!!.name,
            isExpired = booking.isExpired()
        )
    }

    fun toEntity(request: CreateBookingRequest): Booking {
        return Booking().apply {
            userId = request.userId
            startTime = request.startTime
            endTime = request.endTime
        }
    }
}