package com.example.storeservice.dto.request;

import com.example.storeservice.model.enums.ProductCategory;
import com.example.storeservice.model.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductUpdateRequest {
    @Size(max = 255) private String name;
    @Size(max = 2000) private String description;
    private ProductCategory category;
    @Size(max = 1024) private String imageUrl;
    @Min(0) private Integer stockQuantity;
    @DecimalMin("0.00") private BigDecimal price;
    private ProductStatus status;
}
