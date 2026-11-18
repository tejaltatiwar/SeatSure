package com.seatsure.booking;

public enum BookingStatus {
    /** Seats are held; waiting for payment until holdExpiresAt. */
    PENDING_PAYMENT,
    CONFIRMED,
    /** Hold ran out before payment arrived. */
    EXPIRED,
    /** Cancelled by the customer while still unpaid. */
    CANCELLED
}
