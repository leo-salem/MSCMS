package com.example.storeservice.dto.request;

import com.example.storeservice.model.enums.ProductCategory;
import com.example.storeservice.model.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductRequest {
    @NotBlank @Size(max = 255) private String name;
    @Size(max = 2000) private String description;
    @NotNull private ProductCategory category;
    @Size(max = 1024) private String imageUrl;
    @NotNull @Min(0) private Integer stockQuantity;
    @NotNull @DecimalMin("0.00") private BigDecimal price;
    private ProductStatus status;  // defaults to ACTIVE
}
