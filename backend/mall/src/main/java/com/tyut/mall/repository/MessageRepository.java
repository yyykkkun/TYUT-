package com.tyut.mall.repository;

import com.tyut.mall.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByUserIdOrderByCreatedAtDesc(Long userId);

    int countByUserIdAndRead(Long userId, Integer read);
}
