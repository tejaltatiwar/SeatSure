package com.seatsure.payment.gateway;

public record PaymentResult(boolean approved, String gatewayReference, String declineReason) {

    public static PaymentResult approved(String gatewayReference) {
        return new PaymentResult(true, gatewayReference, null);
    }

    public static PaymentResult declined(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
