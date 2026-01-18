package com.anshuman.ticketbooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "seats")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;

    private String bookedBy;

    private Instant bookedAt;

    @Version
    private Long version;

    protected Seat() {
    }

    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Long getId() {
        return id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isBooked() {
        return bookedBy != null;
    }

    public void book(String userId) {
        this.bookedBy = userId;
        this.bookedAt = Instant.now();
    }
}



