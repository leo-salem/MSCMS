package com.example.paymentservice.exception.customException;

public class WebhookVerificationException extends PaymentException {
    public WebhookVerificationException(String message) { super(message); }
    public WebhookVerificationException(String message, Throwable cause) { super(message, cause); }
}
