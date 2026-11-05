package com.pear.customerservice.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
}
