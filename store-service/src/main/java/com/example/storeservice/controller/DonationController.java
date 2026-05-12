package com.example.storeservice.controller;

import com.example.storeservice.dto.request.DonationRequest;
import com.example.storeservice.dto.response.ApiResponse;
import com.example.storeservice.dto.response.DonationResponse;
import com.example.storeservice.dto.response.DonationStatsResponse;
import com.example.storeservice.exception.customException.UnauthorizedAccessException;
import com.example.storeservice.service.DonationService;
import com.example.storeservice.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Make donations to the club using your wallet")
public class DonationController {

    private final DonationService donationService;
    private final SecurityService securityService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Make a donation from my wallet")
    public ResponseEntity<ApiResponse<DonationResponse>> donate(@Valid @RequestBody DonationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                donationService.donate(requireCaller(), req), "Thank you for your donation"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my donations")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(donationService.listMyDonations(
                requireCaller(),
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all donations (donor name hidden for anonymous ones unless caller is admin)")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(donationService.listAllDonations(
                isAdmin(),
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Donation analytics (admin)")
    public ResponseEntity<ApiResponse<DonationStatsResponse>> analytics() {
        return ResponseEntity.ok(ApiResponse.success(donationService.getStats()));
    }

    private String requireCaller() {
        String keycloakId = securityService.getCurrentKeycloakId();
        if (keycloakId == null) throw new UnauthorizedAccessException("No authenticated user");
        return keycloakId;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
