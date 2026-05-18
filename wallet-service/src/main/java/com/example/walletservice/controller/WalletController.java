package com.example.walletservice.controller;

import com.example.walletservice.dto.response.ApiResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.dto.response.WalletResponse;
import com.example.walletservice.exception.customException.UnauthorizedAccessException;
import com.example.walletservice.service.SecurityService;
import com.example.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "User wallet operations")
public class WalletController {

    private final WalletService walletService;
    private final SecurityService securityService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get (or auto-create) the caller's wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        String keycloakId = requireCaller();
        WalletResponse wallet = walletService.getOrCreateMyWallet(keycloakId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Paginated transaction history for the caller's wallet")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> myTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String keycloakId = requireCaller();
        Page<TransactionResponse> txns = walletService.listMyTransactions(
                keycloakId, PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(txns));
    }

    private String requireCaller() {
        String keycloakId = securityService.getCurrentKeycloakId();
        if (keycloakId == null) throw new UnauthorizedAccessException("No authenticated user");
        return keycloakId;
    }
}
