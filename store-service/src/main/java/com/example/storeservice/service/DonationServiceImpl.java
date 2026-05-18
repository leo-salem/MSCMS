package com.example.storeservice.service;

import com.example.storeservice.client.WalletInternalClient;
import com.example.storeservice.client.dto.DebitRequest;
import com.example.storeservice.client.dto.WalletApiResponse;
import com.example.storeservice.client.dto.WalletTransactionDto;
import com.example.storeservice.dto.request.DonationRequest;
import com.example.storeservice.dto.response.DonationResponse;
import com.example.storeservice.dto.response.DonationStatsResponse;
import com.example.storeservice.exception.customException.WalletOperationException;
import com.example.storeservice.mapper.DonationMapper;
import com.example.storeservice.model.entity.Donation;
import com.example.storeservice.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final WalletInternalClient walletClient;
    private final DonationMapper mapper;

    @Value("${mscms.store.currency:USD}")
    private String currency;

    @Override
    @Transactional
    public DonationResponse donate(String userKeycloakId, DonationRequest req) {
        String idempotencyKey = "donation-" + userKeycloakId + "-" + UUID.randomUUID();

        Donation donation = donationRepository.save(Donation.builder()
                .userKeycloakId(userKeycloakId)
                .amount(req.getAmount())
                .currency(currency)
                .message(req.getMessage())
                .anonymous(Boolean.TRUE.equals(req.getAnonymous()))
                .idempotencyKey(idempotencyKey)
                .build());

        DebitRequest debit = DebitRequest.builder()
                .userKeycloakId(userKeycloakId)
                .amount(req.getAmount())
                .type("DONATION")
                .idempotencyKey("donation-debit-" + donation.getId())
                .referenceId(String.valueOf(donation.getId()))
                .referenceType("DONATION")
                .description("Donation #" + donation.getId())
                .build();

        WalletApiResponse<WalletTransactionDto> resp;
        try {
            resp = walletClient.debit(debit);
        } catch (RestClientException e) {
            log.error("wallet debit for donation failed", e);
            throw new WalletOperationException("Wallet service unavailable: " + e.getMessage(), e);
        }
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            String msg = resp != null ? resp.getMessage() : "null response";
            throw new WalletOperationException("Wallet debit failed: " + msg);
        }
        donation.setWalletTransactionId(String.valueOf(resp.getData().getId()));
        donationRepository.save(donation);
        log.info("donation created id={} user={} amount={}", donation.getId(), userKeycloakId, req.getAmount());

        DonationResponse out = mapper.toResponse(donation);
        // For the caller (who is the donor) we always return their own id.
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationResponse> listMyDonations(String userKeycloakId, Pageable pageable) {
        return donationRepository.findByUserKeycloakIdOrderByCreatedAtDesc(userKeycloakId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationResponse> listAllDonations(boolean isAdmin, Pageable pageable) {
        Page<DonationResponse> page = donationRepository.findAll(pageable).map(mapper::toResponse);
        if (!isAdmin) {
            page.getContent().forEach(d -> {
                if (Boolean.TRUE.equals(d.getAnonymous())) d.setUserKeycloakId(null);
            });
        }
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public DonationStatsResponse getStats() {
        return DonationStatsResponse.builder()
                .totalAmount(donationRepository.getTotalAmount())
                .totalDonations(donationRepository.getTotalCount())
                .uniqueDonors(donationRepository.getUniqueDonorCount())
                .build();
    }
}
