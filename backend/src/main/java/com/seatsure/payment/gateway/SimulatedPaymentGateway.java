package com.seatsure.payment.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real provider so the project runs with no external account.
 * Retry wraps the circuit breaker's failures: transient errors are retried up to 3 times,
 * and a sustained outage opens the circuit so we fail fast instead of piling up requests.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    static final String DECLINE_TOKEN = "tok_decline";
    static final String TIMEOUT_TOKEN = "tok_timeout";

    @Override
    @Retry(name = "paymentGateway")
    @CircuitBreaker(name = "paymentGateway")
    public PaymentResult charge(String idempotencyKey, BigDecimal amount, String paymentToken) {
        if (TIMEOUT_TOKEN.equals(paymentToken)) {
            log.warn("Simulated gateway timeout for key {}", idempotencyKey);
            throw new PaymentGatewayUnavailableException();
        }
        if (DECLINE_TOKEN.equals(paymentToken)) {
            return PaymentResult.declined("Card was declined");
        }
        return PaymentResult.approved("sim_" + UUID.randomUUID());
    }
}
