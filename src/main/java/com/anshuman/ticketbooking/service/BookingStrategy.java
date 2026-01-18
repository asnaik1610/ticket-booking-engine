package com.anshuman.ticketbooking.service;

public interface BookingStrategy {
    BookingResult bookSeat(BookingCommand command);
}