package com.pear.customerservice.catalog.service;

import com.pear.customerservice.catalog.dao.CategoryRepository;
import com.pear.customerservice.catalog.dao.SkuRepository;
import com.pear.customerservice.catalog.dao.SpuRepository;
import com.pear.customerservice.catalog.dto.CategoryResponse;
import com.pear.customerservice.catalog.dto.SkuDetailResponse;
import com.pear.customerservice.catalog.dto.SpuDetailResponse;
import com.pear.customerservice.catalog.dto.SpuSummaryResponse;
import com.pear.customerservice.catalog.entity.CategoryEntity;
import com.pear.customerservice.catalog.entity.SaleStatus;
import com.pear.customerservice.catalog.entity.SkuEntity;
import com.pear.customerservice.catalog.entity.SpuEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final SpuRepository spuRepository;
    private final SkuRepository skuRepository;

    public List<CategoryResponse> listCategories() {
        //1.按运营配置顺序查询商品分类
        List<CategoryEntity> categories = categoryRepository.findAllByOrderByDisplayOrderAscIdAsc();

        //2.转换为游客可见的分类响应
        return categories.stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    public List<SpuSummaryResponse> listSpus(Long categoryId) {
        //1.按可选分类查询 SPU
        List<SpuEntity> spus = categoryId == null
                ? spuRepository.findAllByOrderByIdAsc()
                : spuRepository.findByCategoryIdOrderByIdAsc(categoryId);

        //2.转换为包含实时可售状态的 SPU 摘要
        return spus.stream()
                .map(this::toSpuSummary)
                .toList();
    }

    public SpuDetailResponse getSpu(Long spuId) {
        //1.查询指定 SPU，不存在时明确返回未找到
        SpuEntity spu = spuRepository.findById(spuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SPU " + spuId + " 不存在"));

        //2.查询该 SPU 下的全部 SKU
        List<SkuEntity> skus = skuRepository.findBySpuIdOrderByIdAsc(spuId);

        //3.转换为包含组合可售状态的 SPU 详情
        List<SkuDetailResponse> skuResponses = skus.stream()
                .map(sku -> new SkuDetailResponse(
                        sku.getId(), spu.getId(), sku.getName(), spu.getProductType(), sku.getPrice(), sku.getAttributes(),
                        sku.getSaleStatus() == SaleStatus.ON_SALE && spu.getSaleStatus() == SaleStatus.ON_SALE,
                        sku.isDemoData()
                ))
                .toList();
        return new SpuDetailResponse(
                spu.getId(), spu.getCategoryId(), spu.getName(), spu.getProductType(), spu.getDescription(),
                spu.getAttributes(), spu.getSaleStatus() == SaleStatus.ON_SALE, spu.isDemoData(), skuResponses
        );
    }

    public SkuDetailResponse getSku(Long skuId) {
        //1.查询指定 SKU，不存在时明确返回未找到
        SkuEntity sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU " + skuId + " 不存在"));

        //2.查询所属 SPU 以计算组合可售状态
        SpuEntity spu = spuRepository.findById(sku.getSpuId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU " + skuId + " 所属 SPU 不存在"));

        //3.返回实时价格、差异属性和组合可售状态
        boolean onSale = sku.getSaleStatus() == SaleStatus.ON_SALE && spu.getSaleStatus() == SaleStatus.ON_SALE;
        return new SkuDetailResponse(
                sku.getId(), spu.getId(), sku.getName(), spu.getProductType(), sku.getPrice(),
                sku.getAttributes(), onSale, sku.isDemoData()
        );
    }

    private SpuSummaryResponse toSpuSummary(SpuEntity spu) {
        return new SpuSummaryResponse(
                spu.getId(),
                spu.getCategoryId(),
                spu.getName(),
                spu.getProductType(),
                spu.getDescription(),
                spu.getAttributes(),
                spu.getSaleStatus() == SaleStatus.ON_SALE,
                spu.isDemoData()
        );
    }
}
