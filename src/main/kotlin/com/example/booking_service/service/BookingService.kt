package com.example.booking_service.service

import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.model.entity.Booking
import java.util.UUID

interface BookingService{
    fun getAllBookings(): List<BookingDTO>
    fun findActiveBooking(id: UUID): BookingDTO?
    fun createBooking(request: CreateBookingRequest): BookingDTO
    fun cancelBooking(id: UUID): BookingDTO
}