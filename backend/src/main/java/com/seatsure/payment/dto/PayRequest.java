package com.seatsure.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code paymentToken} stands in for a card token from a real provider (Stripe, Razorpay).
 * The simulated gateway understands "tok_decline" and "tok_timeout" so failure paths are demoable.
 */
public record PayRequest(@NotBlank String paymentToken) {
}
