package com.seatsure.payment;

import com.seatsure.auth.security.AuthenticatedUser;
import com.seatsure.payment.dto.PayRequest;
import com.seatsure.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 201 on success, 402 when the card is declined. Replaying the same key returns the same answer. */
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody PayRequest request) {
        PaymentResponse response = paymentService.pay(user.userId(), bookingId, idempotencyKey, request);
        HttpStatus status = response.status() == PaymentStatus.SUCCEEDED
                ? HttpStatus.CREATED
                : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(response);
    }
}
