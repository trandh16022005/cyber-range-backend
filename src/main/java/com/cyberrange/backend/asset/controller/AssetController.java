package com.cyberrange.backend.asset.controller;

import com.cyberrange.backend.asset.dto.AssetResponse;
import com.cyberrange.backend.asset.dto.CreateAssetRequest;
import com.cyberrange.backend.asset.dto.UpdateAssetRequest;
import com.cyberrange.backend.asset.dto.UpdateAssetStatusRequest;
import com.cyberrange.backend.asset.service.AssetService;
import com.cyberrange.backend.common.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // 1. Get assets by Cyber Range
    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetResponse>>> getAssets(
            @RequestParam Long cyberRangeId
    ) {
        List<AssetResponse> assets =
                assetService.getAssetsByCyberRange(cyberRangeId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assets retrieved successfully",
                        assets
                )
        );
    }

    // 2. Get asset details
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponse>> getAssetById(
            @PathVariable Long id
    ) {
        AssetResponse asset = assetService.getAssetById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Asset retrieved successfully",
                        asset
                )
        );
    }

    // 3. Create asset
    @PostMapping
    public ResponseEntity<ApiResponse<AssetResponse>> createAsset(
            @RequestBody CreateAssetRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Asset request must not be null"
            );
        }

        AssetResponse asset = assetService.createAsset(
                request.cyberRangeId(),
                request.name(),
                request.assetType(),
                request.hostname(),
                request.ipAddress(),
                request.os()
        );

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Asset created successfully",
                        asset
                )
        );
    }

    // 4. Update asset details
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAsset(
            @PathVariable Long id,
            @RequestBody UpdateAssetRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Asset request must not be null"
            );
        }

        AssetResponse asset = assetService.updateAsset(
                id,
                request.name(),
                request.assetType(),
                request.hostname(),
                request.ipAddress(),
                request.os()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Asset updated successfully",
                        asset
                )
        );
    }

    // 5. Update asset status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAssetStatus(
            @PathVariable Long id,
            @RequestBody UpdateAssetStatusRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Asset status request must not be null"
            );
        }

        AssetResponse asset = assetService.updateAssetStatus(
                id,
                request.status()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Asset status updated successfully",
                        asset
                )
        );
    }

    // 6. Delete asset
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable Long id
    ) {
        assetService.deleteAsset(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Asset deleted successfully"
                )
        );
    }
}
