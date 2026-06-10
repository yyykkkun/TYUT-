package com.tyut.mall.repository;

import com.tyut.mall.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByIsDefaultDesc(Long userId);

    Optional<Address> findByUserIdAndIsDefault(Long userId, Integer isDefault);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = 0 WHERE a.userId = :userId")
    void clearDefaultByUserId(Long userId);
}
