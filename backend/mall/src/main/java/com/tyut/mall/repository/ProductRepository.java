package com.tyut.mall.repository;

import com.tyut.mall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByStatusOrderBySalesDesc(Integer status);

    List<Product> findByStatusOrderByCreatedAtDesc(Integer status);

    List<Product> findByPromotionAndStatus(String promotion, Integer status);

    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Product> findByCategoryIdAndStatusAndIdNotOrderBySalesDesc(Long categoryId, Integer status, Long excludeId);
}
