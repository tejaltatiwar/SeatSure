package com.seatsure.payment.dto;

import com.seatsure.payment.Payment;
import com.seatsure.payment.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String gatewayReference) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getGatewayReference());
    }
}
