package com.anshuman.ticketbooking.service;

import com.anshuman.ticketbooking.dto.SeatDTO;
import com.anshuman.ticketbooking.exception.BookingException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final DatabaseLockingBookingStrategy databaseStrategy;
    private final DistributedLockingBookingStrategy distributedStrategy;
    private final WebSocketService webSocketService;

    public BookingService(
            DatabaseLockingBookingStrategy databaseStrategy,
            DistributedLockingBookingStrategy distributedStrategy,
            WebSocketService webSocketService) {
        this.databaseStrategy = Objects.requireNonNull(databaseStrategy, "databaseStrategy");
        this.distributedStrategy = Objects.requireNonNull(distributedStrategy, "distributedStrategy");
        this.webSocketService = Objects.requireNonNull(webSocketService, "webSocketService");
    }

    public BookingResult bookSeat(BookingCommand command, BookingStrategyType type) {
        validateCommand(command);
        BookingStrategyType strategyType = resolveStrategy(type);

        long startNanos = System.nanoTime();
        try {
            BookingResult result = executeBooking(command, strategyType);
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            log.info(
                    "Seat {} successfully booked by user {} using {} strategy. Latency: {}ms",
                    result.seatId(),
                    result.bookedBy(),
                    strategyType,
                    durationMs);

            publishSeatUpdate(result);
            return result;
        } catch (BookingException ex) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            log.warn(
                    "Booking failed for seat {} by user {} using {} strategy. Latency: {}ms. Cause: {}",
                    command.seatId(),
                    command.userId(),
                    strategyType,
                    durationMs,
                    ex.getMessage());
            throw ex;
        }
    }

    private void validateCommand(BookingCommand command) {
        Objects.requireNonNull(command, "bookingCommand");
        Assert.notNull(command.seatId(), "seatId is required");
        Assert.hasText(command.userId(), "userId is required");
    }

    private BookingStrategyType resolveStrategy(BookingStrategyType type) {
        return Optional.ofNullable(type)
                .orElseThrow(() -> new BookingException("Booking strategy type is required"));
    }

    private BookingResult executeBooking(BookingCommand command, BookingStrategyType type) {
        return switch (type) {
            case DATABASE -> databaseStrategy.bookSeat(command);
            case REDIS -> distributedStrategy.bookSeat(command);
        };
    }

    private void publishSeatUpdate(BookingResult result) {
        // We emit after the booking completes so downstream consumers never see phantom availability.
        SeatDTO status = new SeatDTO(
                result.seatId(),
                result.seatNumber(),
                result.bookedBy(),
                result.bookedAt(),
                result.booked());
        webSocketService.broadcastSeatUpdate(status);
    }
}



