package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.response.PaymentSessionResponse;
import com.example.paymentservice.model.entity.PaymentSession;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentSessionMapper {
    PaymentSessionResponse toResponse(PaymentSession session);
}
