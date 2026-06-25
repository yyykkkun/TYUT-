package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mall_chat_conversation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "buyer_id", "seller_id"}),
        indexes = {
                @Index(name = "idx_chat_conversation_buyer", columnList = "buyer_id"),
                @Index(name = "idx_chat_conversation_seller", columnList = "seller_id"),
                @Index(name = "idx_chat_conversation_updated", columnList = "updated_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "buyer_unread")
    @Builder.Default
    private Integer buyerUnread = 0;

    @Column(name = "seller_unread")
    @Builder.Default
    private Integer sellerUnread = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
