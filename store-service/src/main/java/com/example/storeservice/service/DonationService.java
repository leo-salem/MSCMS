package com.example.storeservice.service;

import com.example.storeservice.dto.request.DonationRequest;
import com.example.storeservice.dto.response.DonationResponse;
import com.example.storeservice.dto.response.DonationStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DonationService {
    DonationResponse donate(String userKeycloakId, DonationRequest req);
    Page<DonationResponse> listMyDonations(String userKeycloakId, Pageable pageable);
    Page<DonationResponse> listAllDonations(boolean isAdmin, Pageable pageable);
    DonationStatsResponse getStats();
}
