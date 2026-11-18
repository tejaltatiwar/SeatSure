package com.seatsure.payment.gateway;

import com.seatsure.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Raised when the provider cannot be reached. Retried, then trips the circuit breaker. */
public class PaymentGatewayUnavailableException extends ApiException {

    public PaymentGatewayUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_UNAVAILABLE",
                "Payment provider is temporarily unavailable. Your seats stay held, please retry shortly.");
    }
}
