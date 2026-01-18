package com.anshuman.ticketbooking.dto;

import java.time.Instant;

public record BookingResponseDTO(
        Long seatId,
        String seatNumber,
        String bookedBy,
        Instant bookedAt,
        boolean booked) {
}
