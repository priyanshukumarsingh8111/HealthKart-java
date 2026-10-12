package com.healthkart.exception;

/**
 * Exception thrown when attempting to book a slot that is already booked (double-booking).
 * Demonstrates domain-specific concurrency and synchronization exception handling.
 */
public class SlotConflictException extends HealthKartException {
    private final String doctorId;
    private final String date;
    private final String time;

    public SlotConflictException(String doctorId, String date, String time) {
        super("Doctor slot at " + time + " on " + date + " is already booked.", "SLOT_CONFLICT");
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
    }

    public String getDoctorId() { return doctorId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}
