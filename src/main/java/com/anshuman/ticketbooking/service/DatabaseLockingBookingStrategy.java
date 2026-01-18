package com.anshuman.ticketbooking.service;

import com.anshuman.ticketbooking.exception.SeatNotFoundException;
import com.anshuman.ticketbooking.exception.SeatOccupiedException;
import com.anshuman.ticketbooking.model.Seat;
import com.anshuman.ticketbooking.repository.SeatRepository;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseLockingBookingStrategy implements BookingStrategy {
    private static final Logger log = LoggerFactory.getLogger(DatabaseLockingBookingStrategy.class);

    private final SeatRepository seatRepository;

    public DatabaseLockingBookingStrategy(SeatRepository seatRepository) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository");
    }

    @Override
    @Transactional
    public BookingResult bookSeat(BookingCommand command) {
        Objects.requireNonNull(command, "bookingCommand");
        long lockStart = System.nanoTime();
        Seat seat = seatRepository.findByIdForUpdate(command.seatId())
                .orElseThrow(() -> new SeatNotFoundException(command.seatId()));
        long lockMs = Duration.ofNanos(System.nanoTime() - lockStart).toMillis();
        log.debug(
                "Seat {} locked via database for user {} in {}ms",
                command.seatId(),
                command.userId(),
                lockMs);

        // We let the database arbitrate concurrency so only one writer can claim the row at a time.
        if (seat.isBooked()) {
            throw SeatOccupiedException.forSeat(seat);
        }

        seat.book(command.userId());
        seatRepository.save(seat);
        return new BookingResult(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getBookedBy(),
                seat.getBookedAt(),
                true);
    }
}



