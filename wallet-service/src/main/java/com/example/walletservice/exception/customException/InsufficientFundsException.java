package com.example.walletservice.exception.customException;

import java.math.BigDecimal;

public class InsufficientFundsException extends WalletException {
    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient wallet balance. Available: %s, Required: %s", available, requested));
    }
    public InsufficientFundsException(String message) { super(message); }
}
