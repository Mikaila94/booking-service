package com.example.booking_service.controller

import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.service.BookingService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/booking")
class BookingController(private val bookingService: BookingService) {

    @GetMapping("/all")
    fun getAllBookings(): ResponseEntity<List<BookingDTO>> {
        return ResponseEntity.ok(bookingService.getAllBookings())
    }

    @GetMapping("/{id}")
    fun getBooking(@PathVariable @NotNull(message= "ID is required") id: UUID): ResponseEntity<BookingDTO> {
        val booking = bookingService.findActiveBooking(id)
        return booking?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/create")
    fun createBooking(@Valid @RequestBody request: CreateBookingRequest): ResponseEntity<BookingDTO>{
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request))

    }

    @PostMapping("/cancel/{id}")
    fun cancelBooking(@PathVariable @NotNull(message= "ID is required") id: UUID): ResponseEntity<BookingDTO> {
        return ResponseEntity.ok(bookingService.cancelBooking(id))
    }
}