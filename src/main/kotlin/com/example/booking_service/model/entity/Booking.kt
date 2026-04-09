package com.example.booking_service.model.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

enum class BookingStatus {
    ACTIVE,
    CANCELLED
}


@Entity
open class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: UUID? = null

    var userId: UUID? = null
    var startTime: Instant? = null
    var endTime: Instant? = null

    @Enumerated(EnumType.STRING)
    var status: BookingStatus? = null

    fun isExpired(): Boolean {
        return endTime?.isBefore(Instant.now()) ?: false
    }

    fun cancel() {
        require(status == BookingStatus.ACTIVE) {
            "Cannot cancel a booking that is not active"
        }
        status = BookingStatus.CANCELLED
    }

    fun activate() {
        require(status == null) {
            "Cannot activate a booking that already has a status"
        }
        require(startTime != null && endTime != null) {
            "Cannot activate a booking without start and end times"
        }
        require(startTime!! < endTime!!){
            "Start time must be before end time"
        }
        status = BookingStatus.ACTIVE
    }
}