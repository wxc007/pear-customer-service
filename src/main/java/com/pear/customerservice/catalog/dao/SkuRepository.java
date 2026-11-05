package com.pear.customerservice.catalog.dao;

import com.pear.customerservice.catalog.entity.SkuEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuRepository extends JpaRepository<SkuEntity, Long> {

    List<SkuEntity> findBySpuIdOrderByIdAsc(Long spuId);
}
