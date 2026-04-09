package com.example.booking_service.model.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@DisplayName("Booking Entity Tests")
class BookingTest {

    @Nested
    @DisplayName("isExpired()")
    inner class IsExpiredTests {

        @Test
        fun `should return false when endTime is in the future`() {
            // Given
            val booking = Booking().apply {
                endTime = Instant.now().plusSeconds(3600)
            }

            // When & Then
            assertFalse(booking.isExpired())
        }

        @Test
        fun `should return true when endTime is in the past`() {
            // Given
            val booking = Booking().apply {
                endTime = Instant.now().minusSeconds(3600)
            }

            // When & Then
            assertTrue(booking.isExpired())
        }

        @Test
        fun `should return false when endTime is null`() {
            // Given
            val booking = Booking()

            // When & Then
            assertFalse(booking.isExpired())
        }
    }

    @Nested
    @DisplayName("activate()")
    inner class ActivateTests {

        @Test
        fun `should activate booking successfully with valid times`() {
            // Given
            val booking = Booking().apply {
                startTime = Instant.now().plusSeconds(3600)
                endTime = Instant.now().plusSeconds(7200)
            }

            // When
            booking.activate()

            // Then
            assertEquals(BookingStatus.ACTIVE, booking.status)
        }

        @Test
        fun `should allow activation with minimal time difference`() {
            // Given
            val startTime = Instant.now().plusSeconds(3600)
            val endTime = startTime.plusNanos(1)
            val booking = Booking().apply {
                this.startTime = startTime
                this.endTime = endTime
            }

            // When
            booking.activate()

            // Then
            assertEquals(BookingStatus.ACTIVE, booking.status)
        }

        @Nested
        @DisplayName("Validation Failures")
        inner class ActivationValidationTests {

            @Test
            fun `should throw exception when missing start time`() {
                // Given
                val booking = Booking().apply {
                    endTime = Instant.now().plusSeconds(7200)
                }

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.activate()
                }
                assertTrue(exception.message!!.contains("Cannot activate a booking without start and end times"))
            }

            @Test
            fun `should throw exception when missing end time`() {
                // Given
                val booking = Booking().apply {
                    startTime = Instant.now().plusSeconds(3600)
                }

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.activate()
                }
                assertTrue(exception.message!!.contains("Cannot activate a booking without start and end times"))
            }

            @Test
            fun `should throw exception when endTime is before startTime`() {
                // Given
                val booking = Booking().apply {
                    startTime = Instant.now().plusSeconds(7200)
                    endTime = Instant.now().plusSeconds(3600) // Before startTime
                }

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.activate()
                }
                assertTrue(exception.message!!.contains("Start time must be before end time"))
            }

            @Test
            fun `should throw exception when booking already has status`() {
                // Given
                val booking = Booking().apply {
                    startTime = Instant.now().plusSeconds(3600)
                    endTime = Instant.now().plusSeconds(7200)
                    status = BookingStatus.ACTIVE
                }

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.activate()
                }
                assertTrue(exception.message!!.contains("Cannot activate a booking that already has a status"))
            }
        }
    }

    @Nested
    @DisplayName("cancel()")
    inner class CancelTests {

        @Test
        fun `should cancel active booking successfully`() {
            // Given
            val booking = Booking().apply {
                status = BookingStatus.ACTIVE
            }

            // When
            booking.cancel()

            // Then
            assertEquals(BookingStatus.CANCELLED, booking.status)
        }

        @Nested
        @DisplayName("Validation Failures")
        inner class CancellationValidationTests {

            @Test
            fun `should throw exception when booking is already cancelled`() {
                // Given
                val booking = Booking().apply {
                    status = BookingStatus.CANCELLED
                }

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.cancel()
                }
                assertTrue(exception.message!!.contains("Cannot cancel a booking that is not active"))
            }

            @Test
            fun `should throw exception when booking has null status`() {
                // Given
                val booking = Booking()

                // When & Then
                val exception = assertThrows<IllegalArgumentException> {
                    booking.cancel()
                }
                assertTrue(exception.message!!.contains("Cannot cancel a booking that is not active"))
            }
        }
    }
}
