package com.tyut.mall.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectCartRequest {
    @NotNull
    private Integer selected;
}
