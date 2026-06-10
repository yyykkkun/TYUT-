package com.tyut.mall.repository;

import com.tyut.mall.entity.BrowseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {

    List<BrowseHistory> findTop12ByUserIdOrderByCreatedAtDesc(Long userId);

    List<BrowseHistory> findTop1ByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
