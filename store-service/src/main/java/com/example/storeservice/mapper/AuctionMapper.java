package com.example.storeservice.mapper;

import com.example.storeservice.dto.request.AuctionRequest;
import com.example.storeservice.dto.response.AuctionResponse;
import com.example.storeservice.dto.response.BidResponse;
import com.example.storeservice.model.entity.AuctionItem;
import com.example.storeservice.model.entity.Bid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuctionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentHighestBid", ignore = true)
    @Mapping(target = "currentHighestBidder", ignore = true)
    @Mapping(target = "currentHighestBidId", ignore = true)
    @Mapping(target = "winnerKeycloakId", ignore = true)
    @Mapping(target = "winnerBidId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AuctionItem toEntity(AuctionRequest req);

    AuctionResponse toResponse(AuctionItem item);
    List<AuctionResponse> toResponseList(List<AuctionItem> items);

    BidResponse toResponse(Bid bid);
    List<BidResponse> toBidList(List<Bid> bids);
}
