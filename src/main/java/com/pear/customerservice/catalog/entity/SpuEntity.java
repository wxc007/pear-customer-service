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

import java.util.Map;

@Entity
@Table(name = "catalog_spu")
@Getter
@Setter
@NoArgsConstructor
public class SpuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus saleStatus;

    @Column(nullable = false, length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private Map<String, String> attributes;

    @Column(nullable = false)
    private boolean demoData;

    public SpuEntity(Long categoryId, String name, ProductType productType, SaleStatus saleStatus,
                     String description, Map<String, String> attributes, boolean demoData) {
        this.categoryId = categoryId;
        this.name = name;
        this.productType = productType;
        this.saleStatus = saleStatus;
        this.description = description;
        this.attributes = attributes;
        this.demoData = demoData;
    }
}
