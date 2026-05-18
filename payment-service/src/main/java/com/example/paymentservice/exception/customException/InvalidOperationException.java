package com.example.paymentservice.exception.customException;

public class InvalidOperationException extends PaymentException {
    public InvalidOperationException(String message) { super(message); }
}
