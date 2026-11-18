package com.seatsure.notification;

/** Delivery channel port. Replace the logging implementation with SES, SendGrid or SMTP to go live. */
public interface NotificationSender {

    void send(String recipientEmail, String subject, String body);
}
