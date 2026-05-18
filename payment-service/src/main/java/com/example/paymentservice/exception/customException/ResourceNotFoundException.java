package com.example.paymentservice.exception.customException;

public class ResourceNotFoundException extends PaymentException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
    public ResourceNotFoundException(String message) { super(message); }
}
