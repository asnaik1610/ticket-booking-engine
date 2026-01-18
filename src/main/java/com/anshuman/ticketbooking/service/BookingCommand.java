package com.anshuman.ticketbooking.service;

public record BookingCommand(Long seatId, String userId) {
}