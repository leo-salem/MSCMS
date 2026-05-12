package com.example.storeservice.mapper;

import com.example.storeservice.dto.response.DonationResponse;
import com.example.storeservice.model.entity.Donation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DonationMapper {
    DonationResponse toResponse(Donation d);
    List<DonationResponse> toResponseList(List<Donation> ds);
}
