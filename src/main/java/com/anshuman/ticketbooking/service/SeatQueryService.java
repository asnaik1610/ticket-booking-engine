package com.anshuman.ticketbooking.service;

import com.anshuman.ticketbooking.dto.SeatDTO;
import com.anshuman.ticketbooking.repository.SeatRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SeatQueryService {
    private static final Logger log = LoggerFactory.getLogger(SeatQueryService.class);

    private final SeatRepository seatRepository;

    public SeatQueryService(SeatRepository seatRepository) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository");
    }

    public List<SeatDTO> fetchAllSeats() {
        log.debug("Loading seat inventory for UI hydration");
        return seatRepository.findAll().stream()
                .map(SeatDTO::from)
                .toList();
    }
}
