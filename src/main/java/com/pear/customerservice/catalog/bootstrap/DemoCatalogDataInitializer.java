package com.pear.customerservice.catalog.bootstrap;

import com.pear.customerservice.catalog.dao.CategoryRepository;
import com.pear.customerservice.catalog.dao.SkuRepository;
import com.pear.customerservice.catalog.dao.SpuRepository;
import com.pear.customerservice.catalog.entity.CategoryEntity;
import com.pear.customerservice.catalog.entity.ProductType;
import com.pear.customerservice.catalog.entity.SaleStatus;
import com.pear.customerservice.catalog.entity.SkuEntity;
import com.pear.customerservice.catalog.entity.SpuEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoCatalogDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final SpuRepository spuRepository;
    private final SkuRepository skuRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        //1.避免重复初始化演示数据
        if (categoryRepository.count() > 0) {
            return;
        }

        //2.写入明确标记的演示商品分类
        CategoryEntity freshPearCategory = categoryRepository.save(new CategoryEntity("鲜梨", 1));
        CategoryEntity pearProductCategory = categoryRepository.save(new CategoryEntity("梨制品", 2));

        //3.写入鲜梨与梨制品 SPU 演示数据
        SpuEntity crownPear = spuRepository.save(new SpuEntity(
                freshPearCategory.getId(), "赵县皇冠梨", ProductType.FRESH_PEAR, SaleStatus.ON_SALE,
                "果肉细脆、汁多味甜，适合家庭鲜食。",
                Map.of("variety", "皇冠梨", "origin", "河北赵县", "storage", "0-4℃冷藏"), true
        ));
        SpuEntity fragrantPear = spuRepository.save(new SpuEntity(
                freshPearCategory.getId(), "玉露香梨", ProductType.FRESH_PEAR, SaleStatus.OFF_SALE,
                "皮薄肉细，当前演示批次已下架。",
                Map.of("variety", "玉露香梨", "origin", "山西隰县", "storage", "0-4℃冷藏"), true
        ));
        SpuEntity pearPaste = spuRepository.save(new SpuEntity(
                pearProductCategory.getId(), "冰糖梨膏", ProductType.PEAR_PRODUCT, SaleStatus.ON_SALE,
                "梨与冰糖制成的冲调梨膏，请按食用说明保存。",
                Map.of("ingredients", "梨、冰糖", "shelfLife", "12个月", "storage", "阴凉干燥处"), true
        ));

        //4.写入包含上下架与价格差异的 SKU 演示数据
        skuRepository.save(new SkuEntity(
                crownPear.getId(), "皇冠梨 2.5kg 家庭装", new BigDecimal("39.90"), SaleStatus.ON_SALE,
                Map.of("netWeight", "2.5kg", "package", "家庭装", "fruitDiameter", "75-80mm"), true
        ));
        skuRepository.save(new SkuEntity(
                crownPear.getId(), "皇冠梨 5kg 礼盒装", new BigDecimal("69.90"), SaleStatus.OFF_SALE,
                Map.of("netWeight", "5kg", "package", "礼盒装", "fruitDiameter", "80-85mm"), true
        ));
        skuRepository.save(new SkuEntity(
                fragrantPear.getId(), "玉露香梨 2.5kg 家庭装", new BigDecimal("49.90"), SaleStatus.ON_SALE,
                Map.of("netWeight", "2.5kg", "package", "家庭装", "fruitDiameter", "75-80mm"), true
        ));
        skuRepository.save(new SkuEntity(
                pearPaste.getId(), "冰糖梨膏 260g 玻璃瓶装", new BigDecimal("29.90"), SaleStatus.ON_SALE,
                Map.of("netWeight", "260g", "package", "玻璃瓶装"), true
        ));
    }
}
