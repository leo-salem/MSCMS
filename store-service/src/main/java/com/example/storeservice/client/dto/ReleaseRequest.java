package com.example.storeservice.client.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReleaseRequest {
    private Long reservationId;
    private String idempotencyKey;
    private String description;
}
