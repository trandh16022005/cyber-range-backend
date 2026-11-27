package com.cyberrange.backend.asset.service;

import com.cyberrange.backend.asset.dto.ManagedServiceResponse;
import com.cyberrange.backend.asset.entity.Asset;
import com.cyberrange.backend.asset.entity.ManagedService;
import com.cyberrange.backend.asset.repository.AssetRepository;
import com.cyberrange.backend.asset.repository.ManagedServiceRepository;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ManagedServiceService {

    private static final List<String> VALID_STATUSES = List.of(
            "RUNNING",
            "STOPPED",
            "FAILED"
    );

    private final ManagedServiceRepository managedServiceRepository;
    private final AssetRepository assetRepository;

    public ManagedServiceService(
            ManagedServiceRepository managedServiceRepository,
            AssetRepository assetRepository
    ) {
        this.managedServiceRepository = managedServiceRepository;
        this.assetRepository = assetRepository;
    }

    // 1. Get services by Asset
    public List<ManagedServiceResponse> getServicesByAsset(Long assetId) {
        validateRequiredId(assetId, "Asset ID");
        findAssetById(assetId);

        return managedServiceRepository
                .findByAsset_IdOrderByNameAsc(assetId)
                .stream()
                .map(ManagedServiceResponse::fromEntity)
                .toList();
    }

    // 2. Get service details
    public ManagedServiceResponse getServiceById(Long id) {
        ManagedService service = findServiceById(id);

        return ManagedServiceResponse.fromEntity(service);
    }

    // 3. Create service
    @Transactional
    public ManagedServiceResponse createService(
            Long assetId,
            String name,
            String serviceType,
            Integer port,
            String protocol
    ) {
        validateRequiredId(assetId, "Asset ID");

        String normalizedName = validateRequiredText(
                name, "Service name", 150
        );

        String normalizedServiceType = validateRequiredText(
                serviceType, "Service type", 50
        );

        Integer normalizedPort = validatePort(port);
        String normalizedProtocol = normalizeOptional(
                protocol, 20, "Protocol"
        );

        Asset asset = findAssetById(assetId);

        validateUniquePortAndProtocol(
                assetId,
                normalizedPort,
                normalizedProtocol,
                null
        );

        ManagedService service = new ManagedService(
                asset,
                normalizedName,
                normalizedServiceType,
                normalizedPort,
                normalizedProtocol
        );

        return ManagedServiceResponse.fromEntity(
                managedServiceRepository.save(service)
        );
    }

    // 4. Update service details
    @Transactional
    public ManagedServiceResponse updateService(
            Long id,
            String name,
            String serviceType,
            Integer port,
            String protocol
    ) {
        ManagedService service = findServiceById(id);

        String normalizedName = validateRequiredText(
                name, "Service name", 150
        );

        String normalizedServiceType = validateRequiredText(
                serviceType, "Service type", 50
        );

        Integer normalizedPort = validatePort(port);
        String normalizedProtocol = normalizeOptional(
                protocol, 20, "Protocol"
        );

        validateUniquePortAndProtocol(
                service.getAsset().getId(),
                normalizedPort,
                normalizedProtocol,
                id
        );

        service.updateDetails(
                normalizedName,
                normalizedServiceType,
                normalizedPort,
                normalizedProtocol
        );

        return ManagedServiceResponse.fromEntity(service);
    }

    // 5. Update service status
    @Transactional
    public ManagedServiceResponse updateServiceStatus(
            Long id,
            String status
    ) {
        ManagedService service = findServiceById(id);

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Service status is required"
            );
        }

        String normalizedStatus = status
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid service status"
            );
        }

        service.updateStatus(normalizedStatus);

        return ManagedServiceResponse.fromEntity(service);
    }

    // 6. Delete service
    @Transactional
    public void deleteService(Long id) {
        ManagedService service = findServiceById(id);

        managedServiceRepository.delete(service);
    }

    private ManagedService findServiceById(Long id) {
        validateRequiredId(id, "Service ID");

        return managedServiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Service not found"
                        )
                );
    }

    private Asset findAssetById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found"
                        )
                );
    }

    private void validateUniquePortAndProtocol(
            Long assetId,
            Integer port,
            String protocol,
            Long currentServiceId
    ) {
        if (port == null) {
            return;
        }

        boolean duplicate;

        if (currentServiceId == null) {
            duplicate = managedServiceRepository
                    .existsByAsset_IdAndPortAndProtocol(
                            assetId,
                            port,
                            protocol
                    );
        } else {
            duplicate = managedServiceRepository
                    .existsByAsset_IdAndPortAndProtocolAndIdNot(
                            assetId,
                            port,
                            protocol,
                            currentServiceId
                    );
        }

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Port and protocol already exist for this Asset"
            );
        }
    }

    private Integer validatePort(Integer port) {
        if (port == null) {
            return null;
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "Port must be between 1 and 65535"
            );
        }

        return port;
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
}
