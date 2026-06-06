package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuPageQueryDTO;
import com.sparkleshop.service.product.dto.admin.AdminSpuStatusUpdateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuUpdateRequest;
import com.sparkleshop.service.product.vo.AdminSpuDetailRespVO;
import com.sparkleshop.service.product.vo.AdminSpuPageRespVO;

public interface ProductAdminService {

    AdminSpuPageRespVO getAdminSpuPage(AdminSpuPageQueryDTO queryDTO);

    Long createSpu(AdminSpuCreateRequest request);

    AdminSpuDetailRespVO getSpuDetail(Long spuId);

    void updateSpu(Long spuId, AdminSpuUpdateRequest request);

    void updateSpuStatus(Long spuId, AdminSpuStatusUpdateRequest request);

    void deleteSpu(Long spuId);
}
