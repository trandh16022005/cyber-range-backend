package com.cyberrange.backend.asset.controller;

import com.cyberrange.backend.asset.dto.CreateManagedServiceRequest;
import com.cyberrange.backend.asset.dto.ManagedServiceResponse;
import com.cyberrange.backend.asset.dto.UpdateManagedServiceRequest;
import com.cyberrange.backend.asset.dto.UpdateManagedServiceStatusRequest;
import com.cyberrange.backend.asset.service.ManagedServiceService;
import com.cyberrange.backend.common.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class ManagedServiceController {

    private final ManagedServiceService managedServiceService;

    public ManagedServiceController(
            ManagedServiceService managedServiceService
    ) {
        this.managedServiceService = managedServiceService;
    }

    // 1. Get services by Asset
    @GetMapping
    public ResponseEntity<ApiResponse<List<ManagedServiceResponse>>> getServices(
            @RequestParam Long assetId
    ) {
        List<ManagedServiceResponse> services =
                managedServiceService.getServicesByAsset(assetId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Services retrieved successfully",
                        services
                )
        );
    }

    // 2. Get service details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagedServiceResponse>> getServiceById(
            @PathVariable Long id
    ) {
        ManagedServiceResponse service =
                managedServiceService.getServiceById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Service retrieved successfully",
                        service
                )
        );
    }

    // 3. Create service
    @PostMapping
    public ResponseEntity<ApiResponse<ManagedServiceResponse>> createService(
            @RequestBody CreateManagedServiceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Service request must not be null"
            );
        }

        ManagedServiceResponse service =
                managedServiceService.createService(
                        request.assetId(),
                        request.name(),
                        request.serviceType(),
                        request.port(),
                        request.protocol()
                );

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Service created successfully",
                        service
                )
        );
    }

    // 4. Update service details
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagedServiceResponse>> updateService(
            @PathVariable Long id,
            @RequestBody UpdateManagedServiceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Service request must not be null"
            );
        }

        ManagedServiceResponse service =
                managedServiceService.updateService(
                        id,
                        request.name(),
                        request.serviceType(),
                        request.port(),
                        request.protocol()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Service updated successfully",
                        service
                )
        );
    }

    // 5. Update service status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ManagedServiceResponse>> updateServiceStatus(
            @PathVariable Long id,
            @RequestBody UpdateManagedServiceStatusRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Service status request must not be null"
            );
        }

        ManagedServiceResponse service =
                managedServiceService.updateServiceStatus(
                        id,
                        request.status()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Service status updated successfully",
                        service
                )
        );
    }

    // 6. Delete service
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long id
    ) {
        managedServiceService.deleteService(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Service deleted successfully"
                )
        );
    }
}

