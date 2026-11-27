package com.cyberrange.backend.asset.entity;

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
@Table(name = "services")
public class ManagedService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "port")
    private Integer port;

    @Column(name = "protocol", length = 20)
    private String protocol;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "STOPPED";

    protected ManagedService() {
        // Required by JPA
    }

    public ManagedService(
            Asset asset,
            String name,
            String serviceType,
            Integer port,
            String protocol
    ) {
        this.asset = asset;
        this.name = name;
        this.serviceType = serviceType;
        this.port = port;
        this.protocol = protocol;
    }

    public Long getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public String getName() {
        return name;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Integer getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getStatus() {
        return status;
    }
}
