package com.tyut.mall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeCouponRequest {
    @NotNull
    @Min(1)
    private Integer points;
}
