package com.example.walletservice.controller;

import com.example.walletservice.dto.internal.*;
import com.example.walletservice.dto.response.ApiResponse;
import com.example.walletservice.dto.response.ReservationResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Service-to-service API. Guarded by the X-Internal-Service-Token header
 * (granted ROLE_INTERNAL_SERVICE via InternalAuthFilter).
 */
@RestController
@RequestMapping("/internal/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@Tag(name = "Wallet Internal API", description = "Service-to-service wallet operations")
public class WalletInternalController {

    private final WalletService walletService;

    @PostMapping("/credit")
    @Operation(summary = "Credit available_balance (idempotent on idempotencyKey)")
    public ResponseEntity<ApiResponse<TransactionResponse>> credit(@Valid @RequestBody CreditRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.credit(req)));
    }

    @PostMapping("/debit")
    @Operation(summary = "Debit available_balance (idempotent)")
    public ResponseEntity<ApiResponse<TransactionResponse>> debit(@Valid @RequestBody DebitRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.debit(req)));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Move funds from available_balance to reserved_balance")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(@Valid @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.reserve(req)));
    }

    @PostMapping("/release")
    @Operation(summary = "Release a reservation back to available_balance")
    public ResponseEntity<ApiResponse<TransactionResponse>> release(@Valid @RequestBody ReleaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.release(req)));
    }

    @PostMapping("/capture")
    @Operation(summary = "Permanently consume a reservation (e.g. auction win)")
    public ResponseEntity<ApiResponse<TransactionResponse>> capture(@Valid @RequestBody CaptureRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.capture(req)));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund available_balance (idempotent)")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(@Valid @RequestBody RefundRequest req) {
        return ResponseEntity.ok(ApiResponse.success(walletService.refund(req)));
    }
}
