package com.example.mlmodelservice;

import com.example.mlmodelservice.config.MlSeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(MlSeedProperties.class)
public class MlModelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlModelServiceApplication.class, args);
    }
}
