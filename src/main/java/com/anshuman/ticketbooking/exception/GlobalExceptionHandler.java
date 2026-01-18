package com.anshuman.ticketbooking.exception;

import com.anshuman.ticketbooking.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Objects;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter bookingConflictCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.bookingConflictCounter = Counter.builder("booking.conflict.total")
                .description("Total number of booking conflicts")
                .register(meterRegistry);
    }

    @ExceptionHandler(SeatOccupiedException.class)
    public ResponseEntity<ApiErrorResponse> handleSeatOccupied(
            SeatOccupiedException ex,
            HttpServletRequest request) {
        bookingConflictCounter.increment();
        log.info(
                "Booking conflict for seat {} (bookedBy={}, bookedAt={}) on path {}",
                ex.getSeatId(),
                ex.getBookedBy(),
                ex.getBookedAt(),
                request.getRequestURI());
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "SEAT_OCCUPIED",
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}



