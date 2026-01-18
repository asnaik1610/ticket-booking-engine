package com.anshuman.ticketbooking.exception;

import com.anshuman.ticketbooking.model.Seat;
import java.time.Instant;
import java.util.Objects;

public class SeatOccupiedException extends BookingException {
    private final Long seatId;
    private final String bookedBy;
    private final Instant bookedAt;

    public SeatOccupiedException(Long seatId, String bookedBy, Instant bookedAt) {
        super(buildMessage(seatId, bookedBy, bookedAt));
        this.seatId = seatId;
        this.bookedBy = bookedBy;
        this.bookedAt = bookedAt;
    }

    public static SeatOccupiedException forSeat(Seat seat) {
        Objects.requireNonNull(seat, "seat");
        return new SeatOccupiedException(seat.getId(), seat.getBookedBy(), seat.getBookedAt());
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    private static String buildMessage(Long seatId, String bookedBy, Instant bookedAt) {
        String resolvedUser = bookedBy == null ? "unknown user" : bookedBy;
        String resolvedTime = bookedAt == null ? "unknown time" : bookedAt.toString();
        return String.format("Seat %s was already claimed by %s at %s", seatId, resolvedUser, resolvedTime);
    }
}


