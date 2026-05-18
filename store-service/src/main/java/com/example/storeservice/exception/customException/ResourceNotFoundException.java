package com.example.storeservice.exception.customException;

public class ResourceNotFoundException extends StoreException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
    public ResourceNotFoundException(String message) { super(message); }
}
