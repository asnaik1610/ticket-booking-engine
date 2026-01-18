package com.anshuman.ticketbooking.exception;

public class SeatNotFoundException extends BookingException {
    private final Long seatId;

    public SeatNotFoundException(Long seatId) {
        super(String.format("Seat %s not found while processing booking request", seatId));
        this.seatId = seatId;
    }

    public Long getSeatId() {
        return seatId;
    }
}


