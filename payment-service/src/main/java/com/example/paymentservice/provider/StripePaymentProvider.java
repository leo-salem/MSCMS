package com.example.paymentservice.provider;

import com.example.paymentservice.config.StripeConfig;
import com.example.paymentservice.exception.customException.PaymentProviderException;
import com.example.paymentservice.exception.customException.WebhookVerificationException;
import com.example.paymentservice.model.enums.PaymentProvider;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProviderAdapter {

    private final StripeConfig stripeConfig;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public CheckoutSession createCheckoutSession(BigDecimal amount,
                                                 String currency,
                                                 String userKeycloakId,
                                                 String idempotencyKey,
                                                 String successUrl,
                                                 String cancelUrl,
                                                 Map<String, String> metadata) {
        long amountInMinorUnits = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String stripeCurrency = (currency == null ? stripeConfig.getCurrency() : currency).toLowerCase();

        Map<String, String> sessionMetadata = new HashMap<>();
        sessionMetadata.put("userKeycloakId", userKeycloakId);
        sessionMetadata.put("mscmsIdempotencyKey", idempotencyKey);
        if (metadata != null) sessionMetadata.putAll(metadata);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(userKeycloakId)
                .putAllMetadata(sessionMetadata)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(stripeCurrency)
                                .setUnitAmount(amountInMinorUnits)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("MSCMS Wallet Top-Up")
                                        .setDescription("Wallet charge of " + amount + " " + stripeCurrency.toUpperCase())
                                        .build())
                                .build())
                        .build())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putAllMetadata(sessionMetadata)
                        .build())
                .build();

        RequestOptions opts = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();

        try {
            Session session = Session.create(params, opts);
            long expiresAt = session.getExpiresAt() != null ? session.getExpiresAt() : (System.currentTimeMillis() / 1000 + 1800);
            return new CheckoutSession(session.getId(), session.getUrl(), expiresAt);
        } catch (StripeException e) {
            log.error("Stripe createSession failed: {}", e.getMessage(), e);
            throw new PaymentProviderException("Stripe session creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookPayload verifyWebhook(String rawPayload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(rawPayload, signatureHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new WebhookVerificationException("Stripe signature verification failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WebhookVerificationException("Stripe webhook parsing failed: " + e.getMessage(), e);
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<com.stripe.model.StripeObject> obj = deserializer.getObject();

        String sessionId = null;
        String paymentIntentId = null;
        String paymentStatus = null;
        BigDecimal amount = BigDecimal.ZERO;
        String currency = stripeConfig.getCurrency();
        Map<String, String> metadata = new HashMap<>();

        if (obj.isPresent() && obj.get() instanceof Session s) {
            sessionId = s.getId();
            paymentIntentId = s.getPaymentIntent();
            paymentStatus = s.getPaymentStatus();
            if (s.getAmountTotal() != null) {
                amount = BigDecimal.valueOf(s.getAmountTotal()).movePointLeft(2);
            }
            if (s.getCurrency() != null) currency = s.getCurrency();
            if (s.getMetadata() != null) metadata.putAll(s.getMetadata());
        } else if (obj.isPresent() && obj.get() instanceof com.stripe.model.PaymentIntent pi) {
            paymentIntentId = pi.getId();
            paymentStatus = pi.getStatus();
            if (pi.getAmountReceived() != null) {
                amount = BigDecimal.valueOf(pi.getAmountReceived()).movePointLeft(2);
            }
            if (pi.getCurrency() != null) currency = pi.getCurrency();
            if (pi.getMetadata() != null) metadata.putAll(pi.getMetadata());
        }

        return new WebhookPayload(
                event.getId(),
                event.getType(),
                sessionId,
                paymentIntentId,
                paymentStatus,
                amount,
                currency,
                metadata,
                rawPayload
        );
    }
}
