package com.cyberrange.backend.asset.service;

import com.cyberrange.backend.asset.dto.AssetResponse;
import com.cyberrange.backend.asset.entity.Asset;
import com.cyberrange.backend.asset.repository.AssetRepository;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.lab.entity.CyberRange;
import com.cyberrange.backend.lab.repository.CyberRangeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private static final List<String> VALID_STATUSES = List.of(
            "ONLINE",
            "OFFLINE",
            "MAINTENANCE"
    );

    private final AssetRepository assetRepository;
    private final CyberRangeRepository cyberRangeRepository;

    public AssetService(
            AssetRepository assetRepository,
            CyberRangeRepository cyberRangeRepository
    ) {
        this.assetRepository = assetRepository;
        this.cyberRangeRepository = cyberRangeRepository;
    }

    // 1. Get assets by Cyber Range
    public List<AssetResponse> getAssetsByCyberRange(Long cyberRangeId) {
        validateRequiredId(cyberRangeId, "Cyber Range ID");
        findCyberRangeById(cyberRangeId);

        return assetRepository
                .findByCyberRange_IdOrderByNameAsc(cyberRangeId)
                .stream()
                .map(AssetResponse::fromEntity)
                .toList();
    }

    // 2. Get asset details
    public AssetResponse getAssetById(Long id) {
        Asset asset = findAssetById(id);

        return AssetResponse.fromEntity(asset);
    }

    // 3. Create asset
    @Transactional
    public AssetResponse createAsset(
            Long cyberRangeId,
            String name,
            String assetType,
            String hostname,
            String ipAddress,
            String os
    ) {
        validateRequiredId(cyberRangeId, "Cyber Range ID");

        String normalizedName = validateRequiredText(
                name, "Asset name", 150
        );

        String normalizedAssetType = validateRequiredText(
                assetType, "Asset type", 50
        );

        String normalizedHostname = normalizeOptional(
                hostname, 255, "Hostname"
        );

        String normalizedIpAddress = normalizeOptionalIp(ipAddress);

        String normalizedOs = normalizeOptional(
                os, 100, "Operating system"
        );

        CyberRange cyberRange = findCyberRangeById(cyberRangeId);

        if (!"ACTIVE".equals(cyberRange.getStatus())) {
            throw new BusinessException(
                    "Only ACTIVE Cyber Ranges can be used to create assets"
            );
        }

        validateUniqueIp(cyberRangeId, normalizedIpAddress, null);

        Asset asset = new Asset(
                cyberRange,
                normalizedName,
                normalizedAssetType,
                normalizedHostname,
                normalizedIpAddress,
                normalizedOs
        );

        return AssetResponse.fromEntity(
                assetRepository.save(asset)
        );
    }

    // 4. Update asset details
    @Transactional
    public AssetResponse updateAsset(
            Long id,
            String name,
            String assetType,
            String hostname,
            String ipAddress,
            String os
    ) {
        Asset asset = findAssetById(id);

        String normalizedName = validateRequiredText(
                name, "Asset name", 150
        );

        String normalizedAssetType = validateRequiredText(
                assetType, "Asset type", 50
        );

        String normalizedHostname = normalizeOptional(
                hostname, 255, "Hostname"
        );

        String normalizedIpAddress = normalizeOptionalIp(ipAddress);

        String normalizedOs = normalizeOptional(
                os, 100, "Operating system"
        );

        validateUniqueIp(
                asset.getCyberRange().getId(),
                normalizedIpAddress,
                id
        );

        asset.updateDetails(
                normalizedName,
                normalizedAssetType,
                normalizedHostname,
                normalizedIpAddress,
                normalizedOs
        );

        return AssetResponse.fromEntity(asset);
    }

    // 5. Update asset status
    @Transactional
    public AssetResponse updateAssetStatus(
            Long id,
            String status
    ) {
        Asset asset = findAssetById(id);

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Asset status is required"
            );
        }

        String normalizedStatus = status
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid asset status"
            );
        }

        asset.updateStatus(normalizedStatus);

        return AssetResponse.fromEntity(asset);
    }

    // 6. Delete asset
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = findAssetById(id);

        assetRepository.delete(asset);
    }

    // Find asset or throw 404
    private Asset findAssetById(Long id) {
        validateRequiredId(id, "Asset ID");

        return assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found")
                );
    }

    // Find Cyber Range or throw 404
    private CyberRange findCyberRangeById(Long id) {
        return cyberRangeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cyber Range not found")
                );
    }

    // Validate unique IP within the same Cyber Range
    private void validateUniqueIp(
            Long cyberRangeId,
            String ipAddress,
            Long currentAssetId
    ) {
        if (ipAddress == null) {
            return;
        }

        boolean duplicate;

        if (currentAssetId == null) {
            duplicate = assetRepository
                    .existsByCyberRange_IdAndIpAddress(
                            cyberRangeId,
                            ipAddress
                    );
        } else {
            duplicate = assetRepository
                    .existsByCyberRange_IdAndIpAddressAndIdNot(
                            cyberRangeId,
                            ipAddress,
                            currentAssetId
                    );
        }

        if (duplicate) {
            throw new BusinessException(
                    "IP address already exists in this Cyber Range"
            );
        }
    }

    private void validateRequiredId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName
            );
        }
    }

    private String validateRequiredText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private String normalizeOptional(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private String normalizeOptionalIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        return ipAddress.trim();
    }
}
