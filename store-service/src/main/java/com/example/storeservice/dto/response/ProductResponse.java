package com.example.storeservice.dto.response;

import com.example.storeservice.model.enums.ProductCategory;
import com.example.storeservice.model.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private ProductCategory category;
    private String imageUrl;
    private Integer stockQuantity;
    private BigDecimal price;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
