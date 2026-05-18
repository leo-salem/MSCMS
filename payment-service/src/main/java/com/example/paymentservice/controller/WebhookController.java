package com.example.paymentservice.controller;

import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment provider webhook receivers")
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Receives Stripe webhook deliveries. Signature is verified inside the service.
     * Must be reachable without JWT auth.
     */
    @PostMapping(value = "/stripe", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Stripe webhook endpoint (signature-verified). Configure this URL in your Stripe dashboard.")
    public ResponseEntity<String> stripe(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }
}
