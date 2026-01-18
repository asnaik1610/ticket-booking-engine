package com.anshuman.ticketbooking.controller;

import com.anshuman.ticketbooking.dto.BookingRequestDTO;
import com.anshuman.ticketbooking.dto.BookingResponseDTO;
import com.anshuman.ticketbooking.service.BookingCommand;
import com.anshuman.ticketbookingg.service.BookingResult;
import com.anshuman.ticketbooking.service.BookingService;
import com.anshuman.ticketbooking.service.BookingStrategyType;
import java.util.Optional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.Assert;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService");
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> book(@RequestBody BookingRequestDTO bookingRequest) {
        Assert.notNull(bookingRequest, "bookingRequest is required");
        BookingStrategyType strategy = Optional.ofNullable(bookingRequest.strategy())
                .orElse(BookingStrategyType.REDIS);
        log.info(
                "Booking request received for seat {} by user {} using {} strategy",
                bookingRequest.seatId(),
                bookingRequest.userId(),
                strategy);
        BookingResult result = bookingService.bookSeat(
                new BookingCommand(bookingRequest.seatId(), bookingRequest.userId()),
                strategy);

        BookingResponseDTO response = new BookingResponseDTO(
                result.seatId(),
                result.seatNumber(),
                result.bookedBy(),
                result.bookedAt(),
                result.booked());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


