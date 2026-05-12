package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.CreateChargeRequest;
import com.example.paymentservice.dto.response.ApiResponse;
import com.example.paymentservice.dto.response.PaymentSessionResponse;
import com.example.paymentservice.exception.customException.InvalidOperationException;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Create hosted checkout sessions to charge the wallet")
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityService securityService;

    @PostMapping("/charge")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a Stripe Checkout Session to top-up the wallet. Returns a checkout URL.")
    public ResponseEntity<ApiResponse<PaymentSessionResponse>> charge(@Valid @RequestBody CreateChargeRequest req) {
        String keycloakId = requireCaller();
        PaymentSessionResponse session = paymentService.createCharge(keycloakId, req);
        return ResponseEntity.ok(ApiResponse.success(session, "Checkout session created"));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the status of one of my payment sessions")
    public ResponseEntity<ApiResponse<PaymentSessionResponse>> getSession(@PathVariable Long id) {
        String keycloakId = requireCaller();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getMySession(keycloakId, id)));
    }

    private String requireCaller() {
        String keycloakId = securityService.getCurrentKeycloakId();
        if (keycloakId == null) throw new InvalidOperationException("No authenticated user");
        return keycloakId;
    }
}
