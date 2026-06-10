package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {
    private String id;
    private String title;
    private String body;
    private String type;
    private Boolean read;
    private String createdAt;
}
