package com.pear.customerservice.catalog.controller;

import com.pear.customerservice.catalog.dto.CategoryResponse;
import com.pear.customerservice.catalog.dto.SkuDetailResponse;
import com.pear.customerservice.catalog.dto.SpuDetailResponse;
import com.pear.customerservice.catalog.dto.SpuSummaryResponse;
import com.pear.customerservice.catalog.service.CatalogService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        //查询商品分类供游客浏览
        return catalogService.listCategories();
    }

    @GetMapping("/spus")
    public List<SpuSummaryResponse> listSpus(@RequestParam(required = false) Long categoryId) {
        //按可选分类查询 SPU 供游客浏览
        return catalogService.listSpus(categoryId);
    }

    @GetMapping("/spus/{spuId}")
    public SpuDetailResponse getSpu(@PathVariable Long spuId) {
        //查询 SPU 及其全部 SKU 供游客选择规格
        return catalogService.getSpu(spuId);
    }

    @GetMapping("/skus/{skuId}")
    public SkuDetailResponse getSku(@PathVariable Long skuId) {
        //查询具体 SKU 的价格、属性和可售状态
        return catalogService.getSku(skuId);
    }
}
