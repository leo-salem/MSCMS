package com.example.walletservice.service;

import com.example.walletservice.dto.internal.*;
import com.example.walletservice.dto.response.ReservationResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.dto.response.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {

    /** Get or auto-create the caller's wallet. */
    WalletResponse getOrCreateMyWallet(String keycloakId);

    /** Admin lookup. */
    WalletResponse getWalletByKeycloakId(String keycloakId);

    Page<TransactionResponse> listMyTransactions(String keycloakId, Pageable pageable);

    /** Credit available_balance. Idempotent on idempotencyKey. */
    TransactionResponse credit(CreditRequest req);

    /** Debit available_balance. Idempotent. Throws InsufficientFundsException if balance < amount. */
    TransactionResponse debit(DebitRequest req);

    /** Move funds from available to reserved. Used by auction bids. Idempotent. */
    ReservationResponse reserve(ReserveRequest req);

    /** Release a reservation back to available_balance. Idempotent. */
    TransactionResponse release(ReleaseRequest req);

    /** Permanently consume a reservation (auction won, etc.). Idempotent. */
    TransactionResponse capture(CaptureRequest req);

    /** Credit available_balance as a refund. Idempotent. */
    TransactionResponse refund(RefundRequest req);

    /** Admin adjustment (positive credits, negative debits). */
    TransactionResponse adminAdjust(String targetKeycloakId, BigDecimal amount, String reason, String idempotencyKey);
}
