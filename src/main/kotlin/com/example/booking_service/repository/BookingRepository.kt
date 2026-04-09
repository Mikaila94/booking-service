package com.example.booking_service.repository

import com.example.booking_service.model.entity.Booking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BookingRepository : JpaRepository<Booking, UUID> {
    // Additional query methods can be defined here if needed
}
