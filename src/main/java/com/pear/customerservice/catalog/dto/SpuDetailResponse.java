package com.pear.customerservice.catalog.dto;

import com.pear.customerservice.catalog.entity.ProductType;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SpuDetailResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private ProductType productType;
    private String description;
    private Map<String, String> attributes;
    private boolean onSale;
    private boolean demoData;
    private List<SkuDetailResponse> skus;
}
