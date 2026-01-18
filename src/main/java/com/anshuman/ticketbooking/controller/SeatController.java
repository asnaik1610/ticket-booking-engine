package com.anshuman.ticketbooking.controller;

import com.anshuman.ticketbooking.dto.SeatDTO;
import com.anshuman.ticketbooking.service.SeatQueryService;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatController {
    private static final Logger log = LoggerFactory.getLogger(SeatController.class);

    private final SeatQueryService seatQueryService;

    public SeatController(SeatQueryService seatQueryService) {
        this.seatQueryService = Objects.requireNonNull(seatQueryService, "seatQueryService");
    }

    @GetMapping
    public List<SeatDTO> list() {
        log.info("Seat inventory requested");
        return seatQueryService.fetchAllSeats();
    }
}


