package com.example.paymentservice.exception.customException;

public class PaymentProviderException extends PaymentException {
    public PaymentProviderException(String message) { super(message); }
    public PaymentProviderException(String message, Throwable cause) { super(message, cause); }
}
