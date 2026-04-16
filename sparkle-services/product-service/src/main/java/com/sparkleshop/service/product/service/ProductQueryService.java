package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.dto.ProductPageQueryDTO;
import com.sparkleshop.service.product.vo.ProductDetailRespVO;
import com.sparkleshop.service.product.vo.ProductHotRespVO;
import com.sparkleshop.service.product.vo.ProductPageRespVO;

import java.util.List;

public interface ProductQueryService {

    ProductPageRespVO getProductPage(ProductPageQueryDTO queryDTO);

    ProductDetailRespVO getProductDetail(Long skuId);

    List<ProductHotRespVO> getHotProducts(Integer limit);
}
