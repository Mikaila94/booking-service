package com.example.booking_service.mapper

import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.model.entity.Booking
import com.example.booking_service.model.entity.BookingStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

@DisplayName("BookingMapper Tests")
class BookingMapperTest {

    @Test
    fun `should map entity to DTO`() {
        // Given
        val entity = Booking().apply {
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            userId = UUID.fromString("987e6543-e21b-12d3-a456-426614174000")
            startTime = Instant.parse("2026-06-15T10:30:00Z")
            endTime = Instant.parse("2026-06-15T12:30:00Z")
            status = BookingStatus.ACTIVE
        }

        val expected = BookingDTO(
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            userId = UUID.fromString("987e6543-e21b-12d3-a456-426614174000"),
            startTime = Instant.parse("2026-06-15T10:30:00Z"),
            endTime = Instant.parse("2026-06-15T12:30:00Z"),
            status = "ACTIVE",
            isExpired = false
        )

        // When
        val actual = BookingMapper.toDTO(entity)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should map request to entity`() {
        // Given
        val request = CreateBookingRequest(
            userId = UUID.fromString("987e6543-e21b-12d3-a456-426614174000"),
            startTime = Instant.parse("2026-06-15T10:30:00Z"),
            endTime = Instant.parse("2026-06-15T12:30:00Z")
        )

        // When
        val entity = BookingMapper.toEntity(request)

        // Then
        assertAll(
            { assertNull(entity.id) },
            { assertEquals(request.userId, entity.userId) },
            { assertEquals(request.startTime, entity.startTime) },
            { assertEquals(request.endTime, entity.endTime) },
            { assertNull(entity.status) }
        )
    }
}
