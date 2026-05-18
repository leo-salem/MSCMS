package com.example.paymentservice.client;

import com.example.paymentservice.dto.internal.CreditRequest;
import com.example.paymentservice.dto.internal.WalletApiResponse;
import com.example.paymentservice.dto.internal.WalletTransactionDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface WalletInternalClient {
    @PostExchange("/internal/wallets/credit")
    WalletApiResponse<WalletTransactionDto> credit(@RequestBody CreditRequest request);
}
