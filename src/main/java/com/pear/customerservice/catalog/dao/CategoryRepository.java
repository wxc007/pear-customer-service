package com.pear.customerservice.catalog.dao;

import com.pear.customerservice.catalog.entity.CategoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByOrderByDisplayOrderAscIdAsc();
}
