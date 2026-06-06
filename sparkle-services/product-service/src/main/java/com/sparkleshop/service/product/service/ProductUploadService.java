package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.vo.AdminProductImageUploadRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface ProductUploadService {

    AdminProductImageUploadRespVO uploadImage(MultipartFile file);
}
