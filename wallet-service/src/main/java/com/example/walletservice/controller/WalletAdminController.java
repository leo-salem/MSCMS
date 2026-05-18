package com.example.walletservice.controller;

import com.example.walletservice.dto.request.AdminAdjustmentRequest;
import com.example.walletservice.dto.response.ApiResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.dto.response.WalletResponse;
import com.example.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallets/admin")
@RequiredArgsConstructor
@Tag(name = "Wallet Admin", description = "Admin-only wallet operations")
public class WalletAdminController {

    private final WalletService walletService;

    @GetMapping("/{keycloakId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Look up any user's wallet (admin)")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable String keycloakId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByKeycloakId(keycloakId)));
    }

    @PostMapping("/{keycloakId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust a wallet balance (audit-logged)")
    public ResponseEntity<ApiResponse<TransactionResponse>> adjust(
            @PathVariable String keycloakId,
            @Valid @RequestBody AdminAdjustmentRequest req) {
        TransactionResponse txn = walletService.adminAdjust(
                keycloakId, req.getAmount(), req.getReason(), "admin-adjust-" + UUID.randomUUID());
        return ResponseEntity.ok(ApiResponse.success(txn, "Wallet adjusted"));
    }
}
