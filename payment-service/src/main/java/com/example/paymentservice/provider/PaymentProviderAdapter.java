package com.example.paymentservice.provider;

import com.example.paymentservice.model.enums.PaymentProvider;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction over the underlying payment gateway. Add a new implementation
 * (e.g., PaymobPaymentProvider) and configure mscms.payment.provider to swap.
 */
public interface PaymentProviderAdapter {

    PaymentProvider getProvider();

    /** Creates a hosted checkout session and returns provider session id + checkout URL. */
    CheckoutSession createCheckoutSession(BigDecimal amount,
                                          String currency,
                                          String userKeycloakId,
                                          String idempotencyKey,
                                          String successUrl,
                                          String cancelUrl,
                                          Map<String, String> metadata);

    /** Verifies webhook signature and returns the parsed event. */
    WebhookPayload verifyWebhook(String rawPayload, String signatureHeader);

    record CheckoutSession(String sessionId, String checkoutUrl, long expiresAtEpochSeconds) {}

    record WebhookPayload(String eventId, String eventType, String sessionId, String paymentIntentId,
                          String paymentStatus, BigDecimal amount, String currency,
                          Map<String, String> metadata, String rawPayload) {}
}
