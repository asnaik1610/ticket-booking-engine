package com.anshuman.ticketbooking.dto;

import com.anshuman.ticketbooking.model.Seat;
import java.time.Instant;

public record SeatDTO(
        Long seatId,
        String seatNumber,
        String bookedBy,
        Instant bookedAt,
        boolean booked) {
    public static SeatDTO from(Seat seat) {
        return new SeatDTO(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getBookedBy(),
                seat.getBookedAt(),
                seat.isBooked());
    }
}
