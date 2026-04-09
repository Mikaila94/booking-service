package com.example.booking_service

import com.example.booking_service.model.dto.BookingDTO
import com.example.booking_service.model.dto.CreateBookingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BookingServiceApplicationTests(@Autowired val template: TestRestTemplate) {

    @Test
    fun `POST create booking should return 201 with ACTIVE status`() {
        val request = CreateBookingRequest(
            userId = UUID.randomUUID(),
            startTime = Instant.now().plusSeconds(3600),
            endTime = Instant.now().plusSeconds(7200)
        )

        val response = template.postForEntity(
            "/api/booking/create",
            request,
            BookingDTO::class.java
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
        assertEquals("ACTIVE", response.body?.status)
    }
}

