package com.example.storeservice.client;

import com.example.storeservice.client.dto.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface WalletInternalClient {

    @PostExchange("/internal/wallets/debit")
    WalletApiResponse<WalletTransactionDto> debit(@RequestBody DebitRequest req);

    @PostExchange("/internal/wallets/credit")
    WalletApiResponse<WalletTransactionDto> credit(@RequestBody CreditRequest req);

    @PostExchange("/internal/wallets/reserve")
    WalletApiResponse<WalletReservationDto> reserve(@RequestBody ReserveRequest req);

    @PostExchange("/internal/wallets/release")
    WalletApiResponse<WalletTransactionDto> release(@RequestBody ReleaseRequest req);

    @PostExchange("/internal/wallets/capture")
    WalletApiResponse<WalletTransactionDto> capture(@RequestBody CaptureRequest req);

    @PostExchange("/internal/wallets/refund")
    WalletApiResponse<WalletTransactionDto> refund(@RequestBody RefundRequest req);
}
