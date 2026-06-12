package com.tyut.mall.repository;

import com.tyut.mall.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByProductIdAndStatus(Long productId, Integer status);
}
