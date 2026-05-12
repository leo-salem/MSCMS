package com.example.storeservice.mapper;

import com.example.storeservice.dto.response.OrderItemResponse;
import com.example.storeservice.dto.response.OrderResponse;
import com.example.storeservice.model.entity.OrderItem;
import com.example.storeservice.model.entity.ProductOrder;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderResponse toResponse(ProductOrder order);
    OrderItemResponse toResponse(OrderItem item);
    List<OrderItemResponse> toItemList(List<OrderItem> items);
    List<OrderResponse> toResponseList(List<ProductOrder> orders);

    @AfterMapping
    default void computeLineTotal(OrderItem item, @MappingTarget OrderItemResponse resp) {
        if (item == null) return;
        resp.setLineTotal(item.getItemPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
