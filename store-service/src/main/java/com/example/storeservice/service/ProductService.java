package com.example.storeservice.service;

import com.example.storeservice.dto.request.ProductRequest;
import com.example.storeservice.dto.request.ProductUpdateRequest;
import com.example.storeservice.dto.response.ProductResponse;
import com.example.storeservice.model.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse create(ProductRequest req);
    ProductResponse update(Long id, ProductUpdateRequest req);
    void delete(Long id);
    ProductResponse get(Long id);
    Page<ProductResponse> list(ProductCategory category, Boolean activeOnly, Pageable pageable);
}
