package com.example.storeservice.exception.customException;

public class WalletOperationException extends StoreException {
    public WalletOperationException(String msg) { super(msg); }
    public WalletOperationException(String msg, Throwable cause) { super(msg, cause); }
}
