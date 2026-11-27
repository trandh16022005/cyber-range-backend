package com.cyberrange.backend.asset.repository;

import com.cyberrange.backend.asset.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByCyberRange_IdOrderByNameAsc(Long cyberRangeId);

    boolean existsByCyberRange_IdAndIpAddress(Long cyberRangeId, String ipAddress);
}
