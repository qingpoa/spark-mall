package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.dto.admin.AdminCategoryCreateRequest;
import com.sparkleshop.service.product.vo.AdminCategoryRespVO;
import com.sparkleshop.service.product.vo.ProductCategoryTreeRespVO;

import java.util.List;

public interface CategoryService {

    List<ProductCategoryTreeRespVO> getCategoryTree();

    List<AdminCategoryRespVO> getAdminCategoryList();

    Long createCategory(AdminCategoryCreateRequest request);
}
