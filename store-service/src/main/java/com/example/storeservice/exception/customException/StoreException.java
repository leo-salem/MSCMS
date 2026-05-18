package com.example.storeservice.exception.customException;

public class StoreException extends RuntimeException {
    public StoreException(String msg) { super(msg); }
    public StoreException(String msg, Throwable cause) { super(msg, cause); }
}
