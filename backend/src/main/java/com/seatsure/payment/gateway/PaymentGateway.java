package com.seatsure.payment.gateway;

import java.math.BigDecimal;

/** Port to the payment provider. Swap the implementation to go live; nothing else changes. */
public interface PaymentGateway {

    /**
     * @param idempotencyKey forwarded to the provider so a retried call never double-charges
     * @throws PaymentGatewayUnavailableException when the provider cannot be reached
     */
    PaymentResult charge(String idempotencyKey, BigDecimal amount, String paymentToken);
}
