package com.pear.customerservice.catalog.dao;

import com.pear.customerservice.catalog.entity.SpuEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpuRepository extends JpaRepository<SpuEntity, Long> {

    List<SpuEntity> findAllByOrderByIdAsc();

    List<SpuEntity> findByCategoryIdOrderByIdAsc(Long categoryId);
}
