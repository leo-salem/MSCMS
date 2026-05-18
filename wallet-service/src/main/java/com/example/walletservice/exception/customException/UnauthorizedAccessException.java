package com.example.walletservice.exception.customException;

public class UnauthorizedAccessException extends WalletException {
    public UnauthorizedAccessException(String message) { super(message); }
}
