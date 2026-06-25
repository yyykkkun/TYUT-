package com.tyut.mall.repository;

import com.tyut.mall.entity.BrowseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {

    List<BrowseHistory> findTop12ByUserIdOrderByCreatedAtDesc(Long userId);

    List<BrowseHistory> findTop1ByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BrowseHistory b where b.userId = :userId and b.productId = :productId")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
