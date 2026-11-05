package com.pear.customerservice.catalog.dto;

import com.pear.customerservice.catalog.entity.ProductType;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class SkuDetailResponse {

    private Long id;
    private Long spuId;
    private String name;
    private ProductType productType;
    private BigDecimal price;
    private Map<String, String> attributes;
    private boolean onSale;
    private boolean demoData;
}
