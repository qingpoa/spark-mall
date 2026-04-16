package com.sparkleshop.service.product.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminSpuPageQueryDTO {

    private String keyword;

    private Long categoryId;

    private Long brandId;

    private Integer status;

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(50)
    private Integer pageSize = 10;
}
