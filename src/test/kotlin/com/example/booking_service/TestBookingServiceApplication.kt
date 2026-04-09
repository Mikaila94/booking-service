package com.example.booking_service

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<BookingServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
