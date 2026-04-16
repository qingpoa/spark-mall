package com.sparkleshop.service.product.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AdminSpuCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long brandId;

    private String description;

    @Size(max = 255)
    private String mainImage;

    private List<String> images;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status = 1;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer isHot = 0;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer isNew = 0;

    @Valid
    @NotEmpty
    private List<SkuItem> skus;

    @Data
    public static class SkuItem {

        @NotBlank
        @Size(max = 255)
        private String name;

        private Map<String, Object> spec;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;

        @DecimalMin("0.00")
        private BigDecimal costPrice;

        @DecimalMin("0.00")
        private BigDecimal weight;

        @Size(max = 255)
        private String image;

        @NotNull
        @Min(0)
        @Max(1)
        private Integer status = 1;

        @NotNull
        @Min(0)
        private Integer stock = 0;

        @NotNull
        @Min(0)
        private Integer lockedStock = 0;
    }
}
