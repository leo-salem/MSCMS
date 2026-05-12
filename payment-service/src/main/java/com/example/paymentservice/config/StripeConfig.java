package com.example.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Getter
public class StripeConfig {

    @Value("${mscms.stripe.api-key}")
    private String apiKey;

    @Value("${mscms.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${mscms.stripe.currency:usd}")
    private String currency;

    @PostConstruct
    void init() {
        Stripe.apiKey = apiKey;
        if (apiKey == null || apiKey.startsWith("sk_test_replace") || apiKey.startsWith("sk_test_x")) {
            log.warn("Stripe API key looks like a placeholder. Real charges will fail. " +
                    "Set STRIPE_API_KEY env var to a real Stripe secret key (test or live).");
        }
        if (webhookSecret == null || webhookSecret.startsWith("whsec_replace")) {
            log.warn("Stripe webhook secret is a placeholder. Webhooks will be rejected. " +
                    "Set STRIPE_WEBHOOK_SECRET env var.");
        }
    }
}
