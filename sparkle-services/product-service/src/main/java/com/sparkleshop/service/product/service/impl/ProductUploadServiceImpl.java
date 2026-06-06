package com.sparkleshop.service.product.service.impl;

import com.sparkleshop.service.product.service.ProductUploadService;
import com.sparkleshop.service.product.support.OssUtils;
import com.sparkleshop.service.product.vo.AdminProductImageUploadRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductUploadServiceImpl implements ProductUploadService {

    private static final String PRODUCT_IMAGE_DIRECTORY = "product";

    private final OssUtils ossUtils;

    @Override
    public AdminProductImageUploadRespVO uploadImage(MultipartFile file) {
        return new AdminProductImageUploadRespVO(ossUtils.uploadImage(PRODUCT_IMAGE_DIRECTORY, file));
    }
}
