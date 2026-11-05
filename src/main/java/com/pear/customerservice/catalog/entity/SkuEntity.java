package com.pear.customerservice.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "catalog_sku")
@Getter
@Setter
@NoArgsConstructor
public class SkuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long spuId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus saleStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private Map<String, String> attributes;

    @Column(nullable = false)
    private boolean demoData;

    public SkuEntity(Long spuId, String name, BigDecimal price, SaleStatus saleStatus,
                     Map<String, String> attributes, boolean demoData) {
        this.spuId = spuId;
        this.name = name;
        this.price = price;
        this.saleStatus = saleStatus;
        this.attributes = attributes;
        this.demoData = demoData;
    }
}
