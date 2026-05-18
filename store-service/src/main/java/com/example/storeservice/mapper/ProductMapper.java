package com.example.storeservice.mapper;

import com.example.storeservice.dto.request.ProductRequest;
import com.example.storeservice.dto.request.ProductUpdateRequest;
import com.example.storeservice.dto.response.ProductResponse;
import com.example.storeservice.model.entity.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest req);

    ProductResponse toResponse(Product product);
    List<ProductResponse> toResponseList(List<Product> products);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ProductUpdateRequest req, @MappingTarget Product product);
}
