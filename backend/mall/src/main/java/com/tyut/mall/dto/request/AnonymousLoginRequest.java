package com.tyut.mall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnonymousLoginRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
