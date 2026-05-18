package com.example.paymentservice.service;

import com.example.paymentservice.dto.request.CreateChargeRequest;
import com.example.paymentservice.dto.response.PaymentSessionResponse;

public interface PaymentService {
    PaymentSessionResponse createCharge(String userKeycloakId, CreateChargeRequest req);
    PaymentSessionResponse getMySession(String userKeycloakId, Long sessionId);
    void handleStripeWebhook(String rawPayload, String stripeSignature);
}
