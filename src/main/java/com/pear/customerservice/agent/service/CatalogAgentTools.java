package com.pear.customerservice.agent.service;

import com.pear.customerservice.catalog.dto.SkuDetailResponse;
import com.pear.customerservice.catalog.dto.SpuSummaryResponse;
import com.pear.customerservice.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogAgentTools {

    private final CatalogService catalogService;

    @Tool(name = "listSpus", description = "查询实时 SPU 列表及可售状态，可按分类编号筛选；不能用于查询政策或执行写操作")
    public List<SpuSummaryResponse> listSpus(
            @ToolParam(required = false, description = "可选的商品分类编号") Long categoryId) {
        //通过公开目录服务读取实时 SPU，不允许模型拼接 SQL
        return catalogService.listSpus(categoryId);
    }

    @Tool(name = "getSku", description = "按 SKU 编号查询实时名称、价格、规格和组合可售状态")
    public SkuDetailResponse getSku(@ToolParam(description = "需要查询的 SKU 编号") Long skuId) {
        //通过公开目录服务读取实时 SKU 价格与可售状态
        return catalogService.getSku(skuId);
    }
}
