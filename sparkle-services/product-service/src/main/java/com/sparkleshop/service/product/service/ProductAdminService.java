package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuPageQueryDTO;
import com.sparkleshop.service.product.vo.AdminSpuPageRespVO;

public interface ProductAdminService {

    AdminSpuPageRespVO getAdminSpuPage(AdminSpuPageQueryDTO queryDTO);

    Long createSpu(AdminSpuCreateRequest request);
}
