package com.example.booking_service.controller

import com.example.booking_service.config.SecurityConfig
import com.example.booking_service.exception.GlobalExceptionHandler
import com.example.booking_service.service.BookingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(BookingController::class)
@Import(GlobalExceptionHandler::class, SecurityConfig::class, GlobalExceptionHandlerTest.MockConfig::class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun bookingService(): BookingService = mockk()
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var bookingService: BookingService

    @Nested
    @DisplayName("NoSuchElementException handler")
    inner class NoSuchElementExceptionHandlerTests {

        @Test
        fun `should map to 404`() {
            every { bookingService.cancelBooking(any()) } throws NoSuchElementException()

            mockMvc.post("/api/booking/cancel/${UUID.randomUUID()}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.message") { exists() }
                }
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException handler")
    inner class IllegalArgumentExceptionHandlerTests {

        @Test
        fun `should map to 400`() {
            every { bookingService.cancelBooking(any()) } throws IllegalArgumentException()

            mockMvc.post("/api/booking/cancel/${UUID.randomUUID()}")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { exists() }
                }
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException handler")
    inner class MethodArgumentNotValidExceptionHandlerTests {

        @Test
        fun `should return 400 with field errors when startTime is in the past`() {
            mockMvc.post("/api/booking/create") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "userId": "${UUID.randomUUID()}",
                        "startTime": "${Instant.now().minusSeconds(3600)}",
                        "endTime": "${Instant.now().plusSeconds(3600)}"
                    }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.errors.startTime") { exists() }
            }
        }

        @Test
        fun `should return 400 with field errors when endTime is before startTime`() {
            mockMvc.post("/api/booking/create") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "userId": "${UUID.randomUUID()}",
                        "startTime": "${Instant.now().plusSeconds(7200)}",
                        "endTime": "${Instant.now().plusSeconds(3600)}"
                    }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.errors.createBookingRequest") { exists() }
            }
        }
    }
}
