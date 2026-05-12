package com.example.walletservice.mapper;

import com.example.walletservice.dto.response.ReservationResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.dto.response.WalletResponse;
import com.example.walletservice.model.entity.Wallet;
import com.example.walletservice.model.entity.WalletReservation;
import com.example.walletservice.model.entity.WalletTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletMapper {

    @Mapping(target = "totalBalance", expression = "java(wallet.getTotalBalance())")
    WalletResponse toResponse(Wallet wallet);

    TransactionResponse toResponse(WalletTransaction txn);
    List<TransactionResponse> toTxnResponseList(List<WalletTransaction> txns);

    ReservationResponse toResponse(WalletReservation reservation);
}
