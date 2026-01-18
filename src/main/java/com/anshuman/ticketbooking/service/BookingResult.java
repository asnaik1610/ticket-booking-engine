package com.anshuman.ticketbooking.service;

import java.time.Instant;

public record BookingResult(
        Long seatId,
        String seatNumber,
        String bookedBy,
        Instant bookedAt,
        boolean booked) {
}