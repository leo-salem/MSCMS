package com.example.paymentservice.service;

import com.example.paymentservice.client.WalletInternalClient;
import com.example.paymentservice.dto.internal.CreditRequest;
import com.example.paymentservice.dto.internal.WalletApiResponse;
import com.example.paymentservice.dto.internal.WalletTransactionDto;
import com.example.paymentservice.dto.request.CreateChargeRequest;
import com.example.paymentservice.dto.response.PaymentSessionResponse;
import com.example.paymentservice.exception.customException.InvalidOperationException;
import com.example.paymentservice.exception.customException.ResourceNotFoundException;
import com.example.paymentservice.mapper.PaymentSessionMapper;
import com.example.paymentservice.model.entity.PaymentSession;
import com.example.paymentservice.model.entity.WebhookEvent;
import com.example.paymentservice.model.enums.PaymentProvider;
import com.example.paymentservice.model.enums.PaymentSessionStatus;
import com.example.paymentservice.provider.PaymentProviderAdapter;
import com.example.paymentservice.repository.PaymentSessionRepository;
import com.example.paymentservice.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentSessionRepository sessionRepository;
    private final WebhookEventRepository webhookRepository;
    private final PaymentProviderAdapter providerAdapter;
    private final WalletInternalClient walletClient;
    private final PaymentSessionMapper mapper;

    @Value("${mscms.payment.success-url}")
    private String defaultSuccessUrl;

    @Value("${mscms.payment.cancel-url}")
    private String defaultCancelUrl;

    @Value("${mscms.stripe.currency:usd}")
    private String defaultCurrency;

    @Value("${mscms.payment.minimum-charge-amount:1.00}")
    private BigDecimal minAmount;

    @Value("${mscms.payment.maximum-charge-amount:10000.00}")
    private BigDecimal maxAmount;

    @Override
    @Transactional
    public PaymentSessionResponse createCharge(String userKeycloakId, CreateChargeRequest req) {
        if (req.getAmount().compareTo(minAmount) < 0 || req.getAmount().compareTo(maxAmount) > 0) {
            throw new InvalidOperationException(
                    String.format("amount must be between %s and %s", minAmount, maxAmount));
        }

        String idempotencyKey = "charge-" + userKeycloakId + "-" + UUID.randomUUID();
        String currency = (req.getCurrency() != null ? req.getCurrency() : defaultCurrency).toUpperCase();
        String successUrl = req.getSuccessUrl() != null ? req.getSuccessUrl() : defaultSuccessUrl;
        String cancelUrl = req.getCancelUrl() != null ? req.getCancelUrl() : defaultCancelUrl;

        PaymentProviderAdapter.CheckoutSession providerSession = providerAdapter.createCheckoutSession(
                req.getAmount(),
                currency,
                userKeycloakId,
                idempotencyKey,
                successUrl,
                cancelUrl,
                null
        );

        PaymentSession session = PaymentSession.builder()
                .userKeycloakId(userKeycloakId)
                .amount(req.getAmount())
                .currency(currency)
                .provider(providerAdapter.getProvider())
                .providerSessionId(providerSession.sessionId())
                .checkoutUrl(providerSession.checkoutUrl())
                .status(PaymentSessionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .expiresAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(providerSession.expiresAtEpochSeconds()), ZoneId.systemDefault()))
                .build();
        session = sessionRepository.saveAndFlush(session);
        log.info("created payment session id={} provider={} amount={} user={}",
                session.getId(), providerAdapter.getProvider(), req.getAmount(), userKeycloakId);
        return mapper.toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSessionResponse getMySession(String userKeycloakId, Long sessionId) {
        PaymentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentSession", "id", sessionId));
        if (!session.getUserKeycloakId().equals(userKeycloakId)) {
            throw new ResourceNotFoundException("PaymentSession", "id", sessionId);
        }
        return mapper.toResponse(session);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String rawPayload, String stripeSignature) {
        PaymentProviderAdapter.WebhookPayload payload = providerAdapter.verifyWebhook(rawPayload, stripeSignature);

        // ---- Idempotency: only one row per (provider, eventId) ----
        WebhookEvent event = webhookRepository.findByProviderAndEventId(PaymentProvider.STRIPE, payload.eventId())
                .orElse(null);
        if (event != null && Boolean.TRUE.equals(event.getProcessed())) {
            log.info("webhook replay ignored eventId={}", payload.eventId());
            return;
        }
        if (event == null) {
            event = WebhookEvent.builder()
                    .provider(PaymentProvider.STRIPE)
                    .eventId(payload.eventId())
                    .eventType(payload.eventType())
                    .payload(rawPayload)
                    .processed(false)
                    .attemptCount(0)
                    .build();
            try {
                event = webhookRepository.saveAndFlush(event);
            } catch (DataIntegrityViolationException race) {
                event = webhookRepository.findByProviderAndEventId(PaymentProvider.STRIPE, payload.eventId())
                        .orElseThrow(() -> new IllegalStateException("Webhook event race lost"));
                if (Boolean.TRUE.equals(event.getProcessed())) return;
            }
        }
        event.setAttemptCount(event.getAttemptCount() + 1);

        try {
            processWebhookEvent(payload);
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            event.setFailureReason(null);
            webhookRepository.save(event);
            log.info("webhook processed eventId={} type={}", payload.eventId(), payload.eventType());
        } catch (Exception ex) {
            event.setFailureReason(ex.getMessage());
            webhookRepository.save(event);
            log.error("webhook processing failed eventId={} type={}", payload.eventId(), payload.eventType(), ex);
            throw ex;
        }
    }

    private void processWebhookEvent(PaymentProviderAdapter.WebhookPayload payload) {
        // We only act on terminal events for the Checkout Session flow.
        switch (payload.eventType()) {
            case "checkout.session.completed" -> handleCompleted(payload);
            case "checkout.session.expired" -> handleExpired(payload);
            case "checkout.session.async_payment_failed",
                 "payment_intent.payment_failed" -> handleFailed(payload);
            default -> log.debug("ignoring webhook event type={}", payload.eventType());
        }
    }

    private void handleCompleted(PaymentProviderAdapter.WebhookPayload payload) {
        PaymentSession session = sessionRepository.findByProviderSessionId(payload.sessionId())
                .orElse(null);
        if (session == null) {
            log.warn("webhook references unknown providerSessionId={} - skipping", payload.sessionId());
            return;
        }
        if (session.getStatus() == PaymentSessionStatus.COMPLETED) {
            log.info("session already COMPLETED id={}", session.getId());
            return;
        }
        if (!"paid".equalsIgnoreCase(payload.paymentStatus())) {
            log.warn("session.completed but payment_status={} for session={}", payload.paymentStatus(), session.getId());
            return;
        }
        if (payload.amount() != null && payload.amount().signum() > 0
                && payload.amount().compareTo(session.getAmount()) != 0) {
            log.error("amount mismatch: expected {} got {} - rejecting", session.getAmount(), payload.amount());
            session.setStatus(PaymentSessionStatus.FAILED);
            session.setFailureReason("Amount mismatch between Stripe and our records");
            sessionRepository.save(session);
            return;
        }

        // Credit the wallet via internal API (idempotent on session.idempotencyKey)
        CreditRequest cr = CreditRequest.builder()
                .userKeycloakId(session.getUserKeycloakId())
                .amount(session.getAmount())
                .type("DEPOSIT")
                .idempotencyKey("wallet-credit-" + session.getIdempotencyKey())
                .externalPaymentId(payload.paymentIntentId() != null ? payload.paymentIntentId() : payload.sessionId())
                .referenceId(String.valueOf(session.getId()))
                .referenceType("PAYMENT")
                .description("Wallet top-up via " + session.getProvider().name())
                .build();

        WalletApiResponse<WalletTransactionDto> creditResp = walletClient.credit(cr);
        if (creditResp == null || !creditResp.isSuccess() || creditResp.getData() == null) {
            throw new IllegalStateException("Wallet credit failed: " +
                    (creditResp != null ? creditResp.getMessage() : "null response"));
        }

        session.setStatus(PaymentSessionStatus.COMPLETED);
        session.setProviderPaymentIntentId(payload.paymentIntentId());
        session.setWalletTransactionId(String.valueOf(creditResp.getData().getId()));
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("wallet credited for session={} walletTxn={}", session.getId(), creditResp.getData().getId());
    }

    private void handleExpired(PaymentProviderAdapter.WebhookPayload payload) {
        sessionRepository.findByProviderSessionId(payload.sessionId()).ifPresent(s -> {
            if (s.getStatus() == PaymentSessionStatus.PENDING) {
                s.setStatus(PaymentSessionStatus.EXPIRED);
                sessionRepository.save(s);
            }
        });
    }

    private void handleFailed(PaymentProviderAdapter.WebhookPayload payload) {
        if (payload.sessionId() != null) {
            sessionRepository.findByProviderSessionId(payload.sessionId()).ifPresent(s -> {
                if (s.getStatus() == PaymentSessionStatus.PENDING) {
                    s.setStatus(PaymentSessionStatus.FAILED);
                    s.setFailureReason("Payment failed at provider");
                    sessionRepository.save(s);
                }
            });
        }
    }
}
