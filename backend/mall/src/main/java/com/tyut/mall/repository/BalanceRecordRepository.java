package com.tyut.mall.repository;

import com.tyut.mall.entity.BalanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BalanceRecordRepository extends JpaRepository<BalanceRecord, Long> {
    List<BalanceRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
