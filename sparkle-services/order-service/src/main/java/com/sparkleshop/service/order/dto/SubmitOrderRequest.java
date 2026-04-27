package com.sparkleshop.service.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SubmitOrderRequest {

    @NotNull
    @Min(1)
    private Long addressId;

    private Long couponId;

    @Size(max = 200)
    private String remark;

    @NotBlank
    private String submitToken;

    @Valid
    @NotEmpty
    private List<SubmitOrderItemRequest> items;
}
