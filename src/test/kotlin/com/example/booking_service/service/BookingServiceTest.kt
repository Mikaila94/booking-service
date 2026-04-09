package com.example.booking_service.service

import com.example.booking_service.model.dto.CreateBookingRequest
import com.example.booking_service.model.entity.Booking
import com.example.booking_service.model.entity.BookingStatus
import com.example.booking_service.repository.BookingRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

/**
 * Unit tests for BookingService business logic.
 *
 * Focus: Testing actual business logic, not framework behavior or mock interactions.
 * - Tests that expired bookings are filtered out
 * - Tests that entity lifecycle methods (activate/cancel) are called
 * - Avoids testing Spring Data JPA or MockK behavior
 */
@DisplayName("BookingService Tests")
class BookingServiceTest {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var bookingService: BookingService

    @BeforeEach
    fun setup() {
        bookingRepository = mockk(relaxed = true)
        bookingService = BookingServiceImplementation(bookingRepository)
    }

    @Nested
    @DisplayName("createBooking()")
    inner class CreateBookingTests {

        @Test
        fun `should activate the booking before saving`() {
            // Given
            val request = CreateBookingRequest(
                userId = UUID.randomUUID(),
                startTime = Instant.now().plusSeconds(3600),
                endTime = Instant.now().plusSeconds(7200)
            )

            val capturedBooking = slot<Booking>()
            every { bookingRepository.save(capture(capturedBooking)) } answers {
                firstArg<Booking>().apply {
                    id = UUID.randomUUID() // Simulate DB setting the ID
                }
            }

            // When
            bookingService.createBooking(request)

            // Then - verify activate() was called (status should be ACTIVE)
            assertEquals(BookingStatus.ACTIVE, capturedBooking.captured.status)
        }
    }

    @Nested
    @DisplayName("findActiveBooking()")
    inner class FindActiveBookingTests {

        @Test
        fun `should return null for expired bookings`() {
            // Given - booking with endTime in the past
            val bookingId = UUID.randomUUID()
            val expiredBooking = Booking().apply {
                id = bookingId
                userId = UUID.randomUUID()
                startTime = Instant.now().minusSeconds(7200)
                endTime = Instant.now().minusSeconds(3600)
                status = BookingStatus.ACTIVE
            }

            every { bookingRepository.findById(bookingId) } returns Optional.of(expiredBooking)

            // When
            val result = bookingService.findActiveBooking(bookingId)

            // Then - should filter out expired booking
            assertNull(result, "Expired booking should not be returned")
        }

        @Test
        fun `should return active non-expired bookings`() {
            // Given - booking with endTime in the future
            val bookingId = UUID.randomUUID()
            val activeBooking = Booking().apply {
                id = bookingId
                userId = UUID.randomUUID()
                startTime = Instant.now().plusSeconds(3600)
                endTime = Instant.now().plusSeconds(7200)
                status = BookingStatus.ACTIVE
            }

            every { bookingRepository.findById(bookingId) } returns Optional.of(activeBooking)

            // When
            val result = bookingService.findActiveBooking(bookingId)

            // Then - should return the booking
            assertNotNull(result)
            assertEquals(bookingId, result?.id)
        }
    }

    @Nested
    @DisplayName("cancelBooking()")
    inner class CancelBookingTests {

        @Test
        fun `should call cancel on the entity`() {
            // Given
            val bookingId = UUID.randomUUID()
            val booking = Booking().apply {
                id = bookingId
                userId = UUID.randomUUID()
                startTime = Instant.now().plusSeconds(3600)
                endTime = Instant.now().plusSeconds(7200)
                status = BookingStatus.ACTIVE
            }

            val capturedBooking = slot<Booking>()
            every { bookingRepository.findById(bookingId) } returns Optional.of(booking)
            every { bookingRepository.save(capture(capturedBooking)) } answers { firstArg() }

            // When
            bookingService.cancelBooking(bookingId)

            // Then - verify cancel() was called (status should be CANCELLED)
            assertEquals(BookingStatus.CANCELLED, capturedBooking.captured.status)
        }
    }
}

