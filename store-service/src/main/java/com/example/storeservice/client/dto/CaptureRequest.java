package com.example.storeservice.client.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CaptureRequest {
    private Long reservationId;
    private String type;
    private String idempotencyKey;
    private String description;
}
