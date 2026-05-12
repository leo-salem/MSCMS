package com.example.storeservice.controller;

import com.example.storeservice.dto.request.AuctionRequest;
import com.example.storeservice.dto.request.PlaceBidRequest;
import com.example.storeservice.dto.response.ApiResponse;
import com.example.storeservice.dto.response.AuctionResponse;
import com.example.storeservice.dto.response.BidResponse;
import com.example.storeservice.exception.customException.UnauthorizedAccessException;
import com.example.storeservice.model.enums.AuctionStatus;
import com.example.storeservice.service.AuctionEventBroadcaster;
import com.example.storeservice.service.AuctionService;
import com.example.storeservice.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Auction items and live bidding")
public class AuctionController {

    private final AuctionService auctionService;
    private final AuctionEventBroadcaster broadcaster;
    private final SecurityService securityService;

    @GetMapping
    @Operation(summary = "List auctions (public)")
    public ResponseEntity<ApiResponse<Page<AuctionResponse>>> list(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.list(status,
                PageRequest.of(page, Math.min(size, 100), Sort.by("auctionEndTime").ascending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get auction by id (public)")
    public ResponseEntity<ApiResponse<AuctionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.get(id)));
    }

    @GetMapping("/{id}/bids")
    @Operation(summary = "List bids on an auction (public, top first)")
    public ResponseEntity<ApiResponse<Page<BidResponse>>> bids(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.listBids(id,
                PageRequest.of(page, Math.min(size, 100)))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create auction item (admin)")
    public ResponseEntity<ApiResponse<AuctionResponse>> create(@Valid @RequestBody AuctionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.create(req), "Auction created"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel an auction (admin). Releases all active reservations.")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        auctionService.cancelAuction(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Auction cancelled"));
    }

    @PostMapping("/{id}/bids")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Place a bid (reserves the amount on the caller's wallet)")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(@PathVariable Long id,
                                                             @Valid @RequestBody PlaceBidRequest req) {
        String kc = securityService.getCurrentKeycloakId();
        if (kc == null) throw new UnauthorizedAccessException("No authenticated user");
        return ResponseEntity.ok(ApiResponse.success(auctionService.placeBid(kc, id, req), "Bid placed"));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream of bid and finalization events for an auction")
    public SseEmitter stream(@PathVariable Long id) {
        return broadcaster.subscribe(id);
    }
}
