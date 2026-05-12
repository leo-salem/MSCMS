package com.example.storeservice.client.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
