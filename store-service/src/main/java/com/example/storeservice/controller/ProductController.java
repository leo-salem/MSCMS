package com.example.storeservice.controller;

import com.example.storeservice.dto.request.ProductRequest;
import com.example.storeservice.dto.request.ProductUpdateRequest;
import com.example.storeservice.dto.response.ApiResponse;
import com.example.storeservice.dto.response.ProductResponse;
import com.example.storeservice.model.enums.ProductCategory;
import com.example.storeservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Store product catalog")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products (public)")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> list(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.list(category, activeOnly,
                        PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id (public)")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(productService.create(req), "Product created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody ProductUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, req), "Product updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete (DISCONTINUE) a product (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product discontinued"));
    }
}
