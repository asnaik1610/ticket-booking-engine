package com.anshuman.ticketbooking.service;

import com.anshuman.ticketbooking.exception.BookingException;
import com.anshuman.ticketbooking.exception.LockAcquisitionException;
import com.anshuman.ticketbooking.exception.SeatNotFoundException;
import com.anshuman.ticketbooking.exception.SeatOccupiedException;
import com.anshuman.ticketbooking.model.Seat;
import com.anshuman.ticketbooking.repository.SeatRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DistributedLockingBookingStrategy implements BookingStrategy {
    private static final Duration LOCK_WAIT = Duration.ofSeconds(2);
    private static final Duration LOCK_LEASE = Duration.ofSeconds(5);
    private static final Logger log = LoggerFactory.getLogger(DistributedLockingBookingStrategy.class);

    private final SeatRepository seatRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public DistributedLockingBookingStrategy(
            SeatRepository seatRepository,
            RedissonClient redissonClient,
            PlatformTransactionManager transactionManager) {
        this.seatRepository = Objects.requireNonNull(seatRepository, "seatRepository");
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
    }

    @Override
    public BookingResult bookSeat(BookingCommand command) {
        Objects.requireNonNull(command, "bookingCommand");
        RLock lock = redissonClient.getLock(lockKey(command.seatId()));
        boolean acquired = false;
        try {
            long lockStart = System.nanoTime();
            // We keep the wait short so callers fail fast, and the lease short to avoid orphaned locks.
            acquired = lock.tryLock(
                    LOCK_WAIT.toSeconds(),
                    LOCK_LEASE.toSeconds(),
                    TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockAcquisitionException(command.seatId(), LOCK_WAIT);
            }
            long lockMs = Duration.ofNanos(System.nanoTime() - lockStart).toMillis();
            log.debug(
                    "Seat {} locked via Redis for user {} in {}ms",
                    command.seatId(),
                    command.userId(),
                    lockMs);

            return transactionTemplate.execute(status -> {
                Seat seat = seatRepository.findById(command.seatId())
                        .orElseThrow(() -> new SeatNotFoundException(command.seatId()));
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
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BookingException("Interrupted while acquiring lock for seat: " + command.seatId());
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    private String lockKey(Long seatId) {
        return "lock:seat:" + seatId;
    }
}
