package com.example.storeservice.service;

import com.example.storeservice.dto.request.ProductRequest;
import com.example.storeservice.dto.request.ProductUpdateRequest;
import com.example.storeservice.dto.response.ProductResponse;
import com.example.storeservice.exception.customException.ResourceNotFoundException;
import com.example.storeservice.mapper.ProductMapper;
import com.example.storeservice.model.entity.Product;
import com.example.storeservice.model.enums.ProductCategory;
import com.example.storeservice.model.enums.ProductStatus;
import com.example.storeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest req) {
        Product p = mapper.toEntity(req);
        if (p.getStatus() == null) p.setStatus(ProductStatus.ACTIVE);
        return mapper.toResponse(repository.save(p));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest req) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        mapper.update(req, p);
        return mapper.toResponse(repository.save(p));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        // Soft-delete pattern: mark DISCONTINUED so existing orders stay intact
        p.setStatus(ProductStatus.DISCONTINUED);
        repository.save(p);
        log.info("product discontinued id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(ProductCategory category, Boolean activeOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return (category == null
                    ? repository.findByStatus(ProductStatus.ACTIVE, pageable)
                    : repository.findByStatusAndCategory(ProductStatus.ACTIVE, category, pageable))
                    .map(mapper::toResponse);
        }
        return repository.findAll(pageable).map(mapper::toResponse);
    }
}
