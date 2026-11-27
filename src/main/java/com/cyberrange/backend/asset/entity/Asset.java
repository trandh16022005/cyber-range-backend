package com.cyberrange.backend.asset.entity;

import com.cyberrange.backend.lab.entity.CyberRange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cyber_range_id", nullable = false)
    private CyberRange cyberRange;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @Column(name = "hostname", length = 255)
    private String hostname;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OFFLINE";

    protected Asset() {
        // Required by JPA
    }

    public Asset(
            CyberRange cyberRange,
            String name,
            String assetType,
            String hostname,
            String ipAddress,
            String os
    ) {
        this.cyberRange = cyberRange;
        this.name = name;
        this.assetType = assetType;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.os = os;
    }

    public Long getId() {
        return id;
    }

    public CyberRange getCyberRange() {
        return cyberRange;
    }

    public String getName() {
        return name;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getOs() {
        return os;
    }

    public String getStatus() {
        return status;
    }

    public void updateDetails(
            String name,
            String assetType,
            String hostname,
            String ipAddress,
            String os
    ) {
        this.name = name;
        this.assetType = assetType;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.os = os;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
