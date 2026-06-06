package com.sparkleshop.service.product.service;

import com.sparkleshop.service.product.service.impl.ProductUploadServiceImpl;
import com.sparkleshop.service.product.support.OssUtils;
import com.sparkleshop.service.product.vo.AdminProductImageUploadRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUploadServiceImplTest {

    @Mock
    private OssUtils ossUtils;
    @Mock
    private MultipartFile file;

    private ProductUploadServiceImpl productUploadService;

    @BeforeEach
    void setUp() {
        productUploadService = new ProductUploadServiceImpl(ossUtils);
    }

    @Test
    void shouldUploadProductImageToProductDirectory() {
        when(ossUtils.uploadImage("product", file)).thenReturn("https://img.sparkmall.test/product/a.png");

        AdminProductImageUploadRespVO response = productUploadService.uploadImage(file);

        assertEquals("https://img.sparkmall.test/product/a.png", response.getUrl());
        verify(ossUtils).uploadImage("product", file);
    }
}
