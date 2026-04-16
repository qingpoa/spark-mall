package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.vo.ProductBrandRespVO;

import java.util.List;

public interface BrandService {

    List<ProductBrandRespVO> getEnabledBrandList(String keyword);
}
