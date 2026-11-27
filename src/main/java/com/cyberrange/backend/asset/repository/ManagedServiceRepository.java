package com.cyberrange.backend.asset.repository;

import com.cyberrange.backend.asset.entity.ManagedService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagedServiceRepository extends JpaRepository<ManagedService, Long> {

    List<ManagedService> findByAsset_IdOrderByNameAsc(Long assetId);

    boolean existsByAsset_IdAndPortAndProtocol(
            Long assetId,
            Integer port,
            String protocol
    );
}
