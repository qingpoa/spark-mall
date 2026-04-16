package com.sparkleshop.service.product.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCategoryCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Min(0)
    private Long parentId = 0L;

    @Min(0)
    private Integer sort = 0;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status = 1;

    @Size(max = 255)
    private String icon;
}
