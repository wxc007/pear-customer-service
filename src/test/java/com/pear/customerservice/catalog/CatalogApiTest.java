package com.pear.customerservice.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers
class CatalogApiTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("pear_customer_service_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestsCanBrowseCategories() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("鲜梨"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("梨制品"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void guestsCanBrowseSpusByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/spus").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("赵县皇冠梨"))
                .andExpect(jsonPath("$[0].productType").value("FRESH_PEAR"))
                .andExpect(jsonPath("$[0].attributes.variety").value("皇冠梨"))
                .andExpect(jsonPath("$[0].onSale").value(true))
                .andExpect(jsonPath("$[0].demoData").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("玉露香梨"))
                .andExpect(jsonPath("$[1].onSale").value(false))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void guestsCanReadFreshPearSkuDetail() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/skus/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.spuId").value(1))
                .andExpect(jsonPath("$.name").value("皇冠梨 2.5kg 家庭装"))
                .andExpect(jsonPath("$.productType").value("FRESH_PEAR"))
                .andExpect(jsonPath("$.price").value(39.90))
                .andExpect(jsonPath("$.attributes.netWeight").value("2.5kg"))
                .andExpect(jsonPath("$.attributes.fruitDiameter").value("75-80mm"))
                .andExpect(jsonPath("$.onSale").value(true))
                .andExpect(jsonPath("$.demoData").value(true));
    }

    @Test
    void guestsCanBrowseSkusWithinSpu() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/spus/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("赵县皇冠梨"))
                .andExpect(jsonPath("$.skus[0].id").value(1))
                .andExpect(jsonPath("$.skus[0].onSale").value(true))
                .andExpect(jsonPath("$.skus[1].id").value(2))
                .andExpect(jsonPath("$.skus[1].onSale").value(false))
                .andExpect(jsonPath("$.skus.length()").value(2));
    }

    @Test
    void skuDetailsReflectBothSpuAndSkuSaleStatus() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/skus/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onSale").value(false));

        mockMvc.perform(get("/api/v1/catalog/skus/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onSale").value(false));
    }

    @Test
    void unknownSkuReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/skus/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("SKU 999 不存在"))
                .andExpect(jsonPath("$.instance").value("/api/v1/catalog/skus/999"));
    }
}
