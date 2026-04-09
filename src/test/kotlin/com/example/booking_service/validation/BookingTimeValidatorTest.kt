package com.example.booking_service.validation

import com.example.booking_service.model.dto.CreateBookingRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

@DisplayName("BookingTimeValidator Tests")
class BookingTimeValidatorTest {

    private val validator = BookingTimeValidator()

    @Nested
    @DisplayName("Valid Time Ranges")
    inner class ValidTimeRangeTests {

        @Test
        fun `should return true when endTime is after startTime`() {
            // Given
            val startTime = Instant.now().plusSeconds(3600)
            val endTime = startTime.plusSeconds(3600)
            val request = CreateBookingRequest(UUID.randomUUID(), startTime, endTime)

            // When
            val result = validator.isValid(request, null)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return true when endTime is one second after startTime`() {
            // Given
            val startTime = Instant.now().plusSeconds(3600)
            val endTime = startTime.plusSeconds(1)
            val request = CreateBookingRequest(UUID.randomUUID(), startTime, endTime)

            // When
            val result = validator.isValid(request, null)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return true when endTime is far in the future`() {
            // Given
            val startTime = Instant.now().plusSeconds(3600)
            val endTime = startTime.plusSeconds(86400 * 365) // One year later
            val request = CreateBookingRequest(UUID.randomUUID(), startTime, endTime)

            // When
            val result = validator.isValid(request, null)

            // Then
            assertTrue(result)
        }
    }

    @Nested
    @DisplayName("Invalid Time Ranges")
    inner class InvalidTimeRangeTests {

        @Test
        fun `should return false when endTime is before startTime`() {
            // Given
            val startTime = Instant.now().plusSeconds(3600)
            val endTime = startTime.minusSeconds(3600)
            val request = CreateBookingRequest(UUID.randomUUID(), startTime, endTime)

            // When
            val result = validator.isValid(request, null)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when endTime equals startTime`() {
            // Given
            val time = Instant.now().plusSeconds(3600)
            val request = CreateBookingRequest(UUID.randomUUID(), time, time)

            // When
            val result = validator.isValid(request, null)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {

        @Test
        fun `should return true when request is null`() {
            // When
            val result = validator.isValid(null, null)

            // Then
            assertTrue(result)
        }
    }
}
