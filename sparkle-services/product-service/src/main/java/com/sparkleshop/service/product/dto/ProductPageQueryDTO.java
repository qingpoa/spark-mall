package com.sparkleshop.service.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductPageQueryDTO {

    private Long categoryId;

    private Long brandId;

    private String keyword;

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(50)
    private Integer pageSize = 10;

    private String sortType = "default";
}
