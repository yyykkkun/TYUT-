package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {
    private String id;
    private String orderId;
    private String userName;
    private String userAvatar;
    private Integer rating;
    private String content;
    private List<String> images;
    private String createdAt;
}
