package com.cyberrange.backend.lab.repository;

import com.cyberrange.backend.lab.entity.CyberRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CyberRangeRepository extends JpaRepository<CyberRange, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<CyberRange> findByStatus(String status);
}
