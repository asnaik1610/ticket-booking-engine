package com.anshuman.ticketbooking.exception;

import java.time.Duration;

public class LockAcquisitionException extends BookingException {
    private final Long seatId;
    private final Duration waitTimeout;

    public LockAcquisitionException(Long seatId, Duration waitTimeout) {
        super(String.format(
                "Distributed lock acquisition timed out after %sms for seat %s",
                waitTimeout.toMillis(),
                seatId));
        this.seatId = seatId;
        this.waitTimeout = waitTimeout;
    }

    public Long getSeatId() {
        return seatId;
    }

    public Duration getWaitTimeout() {
        return waitTimeout;
    }
}



