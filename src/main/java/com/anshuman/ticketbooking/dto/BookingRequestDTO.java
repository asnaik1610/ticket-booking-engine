package com.anshuman.ticketbooking.dto;

import com.anshuman.ticketbooking.service.BookingStrategyType;

public record BookingRequestDTO(Long seatId, String userId, BookingStrategyType strategy) {
}
