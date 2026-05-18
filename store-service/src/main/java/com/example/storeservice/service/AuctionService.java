package com.example.storeservice.service;

import com.example.storeservice.dto.request.AuctionRequest;
import com.example.storeservice.dto.request.PlaceBidRequest;
import com.example.storeservice.dto.response.AuctionResponse;
import com.example.storeservice.dto.response.BidResponse;
import com.example.storeservice.model.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionService {
    AuctionResponse create(AuctionRequest req);
    AuctionResponse get(Long id);
    Page<AuctionResponse> list(AuctionStatus status, Pageable pageable);
    Page<BidResponse> listBids(Long auctionId, Pageable pageable);
    BidResponse placeBid(String userKeycloakId, Long auctionId, PlaceBidRequest req);
    void cancelAuction(Long auctionId);
    void finalizeEndedAuctions();
    void finalizeOne(Long auctionId);
    void activateScheduledAuctions();
}
