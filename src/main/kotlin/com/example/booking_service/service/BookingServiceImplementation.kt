package com.example.booking_service.service

import com.example.booking_service.mapper.BookingMapper
import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.repository.BookingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
@Transactional(readOnly = true)  // Default for all methods
class BookingServiceImplementation(private val bookingRepository: BookingRepository)  : BookingService {

    override fun getAllBookings(): List<BookingDTO> {
        return bookingRepository.findAll()
            .map(BookingMapper::toDTO)
    }

    override fun findActiveBooking(id: UUID): BookingDTO? {
        return  bookingRepository
            .findById(id)
            .orElseThrow { NoSuchElementException("Booking not found: $id") }
            .takeUnless { it.isExpired() }
            ?.let { BookingMapper.toDTO(it) }
    }

    @Transactional  // Overrides class-level readOnly
    override fun createBooking(request: CreateBookingRequest): BookingDTO {
        val booking = BookingMapper.toEntity(request)
        booking.activate()
        return BookingMapper.toDTO(bookingRepository.save(booking))
    }

    @Transactional  // Overrides class-level readOnly
    override fun cancelBooking(id: UUID): BookingDTO {
        val booking = bookingRepository.findById(id)
            .orElseThrow { NoSuchElementException("Booking not found: $id") }
        booking.cancel()
        return BookingMapper.toDTO(bookingRepository.save(booking))
    }
}