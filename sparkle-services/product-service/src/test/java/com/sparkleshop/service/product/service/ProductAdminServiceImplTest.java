package com.sparkleshop.service.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.entity.BrandDO;
import com.sparkleshop.service.product.entity.CategoryDO;
import com.sparkleshop.service.product.entity.SkuDO;
import com.sparkleshop.service.product.entity.SkuStockDO;
import com.sparkleshop.service.product.entity.SpuDO;
import com.sparkleshop.service.product.mapper.BrandMapper;
import com.sparkleshop.service.product.mapper.CategoryMapper;
import com.sparkleshop.service.product.mapper.SkuMapper;
import com.sparkleshop.service.product.mapper.SkuStockMapper;
import com.sparkleshop.service.product.mapper.SpuMapper;
import com.sparkleshop.service.product.service.impl.ProductAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAdminServiceImplTest {

    @Mock
    private SpuMapper spuMapper;
    @Mock
    private SkuMapper skuMapper;
    @Mock
    private SkuStockMapper skuStockMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private ProductAdminServiceImpl productAdminService;

    @BeforeEach
    void setUp() {
        productAdminService = new ProductAdminServiceImpl(
                spuMapper, skuMapper, skuStockMapper, categoryMapper, brandMapper, stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void shouldCreateSpuAndSkuStock() {
        CategoryDO category = new CategoryDO();
        category.setId(1L);
        BrandDO brand = new BrandDO();
        brand.setId(2L);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(brandMapper.selectById(2L)).thenReturn(brand);
        doAnswer(invocation -> {
            SpuDO spu = invocation.getArgument(0);
            spu.setId(100L);
            return 1;
        }).when(spuMapper).insert(any(SpuDO.class));
        doAnswer(invocation -> {
            SkuDO sku = invocation.getArgument(0);
            sku.setId(1000L);
            return 1;
        }).when(skuMapper).insert(any(SkuDO.class));

        AdminSpuCreateRequest request = new AdminSpuCreateRequest();
        request.setName("测试商品");
        request.setCategoryId(1L);
        request.setBrandId(2L);
        request.setMainImage("main.jpg");
        AdminSpuCreateRequest.SkuItem skuItem = new AdminSpuCreateRequest.SkuItem();
        skuItem.setName("默认规格");
        skuItem.setSpec(Map.of("规格", "默认"));
        skuItem.setPrice(new BigDecimal("19.90"));
        skuItem.setStock(10);
        skuItem.setLockedStock(2);
        request.setSkus(List.of(skuItem));

        Long spuId = productAdminService.createSpu(request);

        assertEquals(100L, spuId);
        verify(spuMapper).insert(any(SpuDO.class));
        verify(skuMapper).insert(any(SkuDO.class));
        verify(skuStockMapper).insert(any(SkuStockDO.class));
        verify(stringRedisTemplate).delete("sparkle:product:hot:list");
    }
}
