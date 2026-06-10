package com.tyut.mall.repository;

import com.tyut.mall.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndProductIdAndSpec(Long userId, Long productId, String spec);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.selected = 1")
    void deleteSelectedByUserId(Long userId);
}
