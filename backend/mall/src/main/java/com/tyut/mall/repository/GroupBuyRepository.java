package com.tyut.mall.repository;

import com.tyut.mall.entity.GroupBuy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {
    List<GroupBuy> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);
    List<GroupBuy> findByStatus(String status);
}
