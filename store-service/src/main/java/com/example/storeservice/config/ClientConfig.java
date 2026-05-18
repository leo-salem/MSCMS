package com.example.storeservice.config;

import com.example.storeservice.client.WalletInternalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public WalletInternalClient walletInternalClient(
            RestClient.Builder restClientBuilder,
            @Value("${mscms.internal.token}") String internalToken) {
        RestClient restClient = restClientBuilder
                .baseUrl("lb://WALLET-SERVICE")
                .defaultHeader("X-Internal-Service-Token", internalToken)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(WalletInternalClient.class);
    }
}
