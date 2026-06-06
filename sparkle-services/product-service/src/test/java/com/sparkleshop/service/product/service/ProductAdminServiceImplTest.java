package com.sparkleshop.service.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.service.product.constant.ProductRedisKeys;
import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuStatusUpdateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuUpdateRequest;
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
import com.sparkleshop.service.product.vo.AdminSpuDetailRespVO;
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
        verify(stringRedisTemplate).delete(ProductRedisKeys.PRODUCT_HOT_LIST);
    }

    @Test
    void shouldReturnSpuDetailWithSkuStock() {
        SpuDO spu = buildSpu();
        SkuDO sku = buildSku(10L, 100L);
        SkuStockDO stock = buildStock(1000L, 10L, 20, 3);
        when(spuMapper.selectById(100L)).thenReturn(spu);
        when(skuMapper.selectAdminBySpuId(100L)).thenReturn(List.of(sku));
        when(skuStockMapper.selectBySkuIds(List.of(10L))).thenReturn(List.of(stock));

        AdminSpuDetailRespVO detail = productAdminService.getSpuDetail(100L);

        assertEquals(100L, detail.getId());
        assertEquals(List.of("main-1.jpg"), detail.getImages());
        assertEquals(1, detail.getSkus().size());
        assertEquals(17, detail.getSkus().get(0).getAvailableStock());
        assertEquals("默认", detail.getSkus().get(0).getSpec().get("规格"));
    }

    @Test
    void shouldUpdateSpuAndSyncSkuStock() {
        CategoryDO category = new CategoryDO();
        category.setId(1L);
        BrandDO brand = new BrandDO();
        brand.setId(2L);
        SpuDO spu = buildSpu();
        SkuDO oldSku = buildSku(10L, 100L);
        SkuDO removedSku = buildSku(11L, 100L);
        SkuStockDO oldStock = buildStock(1000L, 10L, 20, 3);
        SkuStockDO removedStock = buildStock(1001L, 11L, 5, 0);

        when(spuMapper.selectById(100L)).thenReturn(spu);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(brandMapper.selectById(2L)).thenReturn(brand);
        when(skuMapper.selectAdminBySpuId(100L)).thenReturn(List.of(oldSku, removedSku));
        when(skuStockMapper.selectBySkuId(any())).thenAnswer(invocation -> {
            Long skuId = invocation.getArgument(0);
            if (Long.valueOf(10L).equals(skuId)) {
                return oldStock;
            }
            if (Long.valueOf(11L).equals(skuId)) {
                return removedStock;
            }
            return null;
        });
        doAnswer(invocation -> {
            SkuDO sku = invocation.getArgument(0);
            sku.setId(12L);
            return 1;
        }).when(skuMapper).insert(any(SkuDO.class));

        AdminSpuUpdateRequest request = buildUpdateRequest();

        productAdminService.updateSpu(100L, request);

        verify(spuMapper).updateById(any(SpuDO.class));
        verify(skuMapper).updateById(any(SkuDO.class));
        verify(skuMapper).insert(any(SkuDO.class));
        verify(skuStockMapper).updateById(oldStock);
        verify(skuStockMapper).deleteById(1001L);
        verify(skuMapper).deleteById(11L);
        verify(stringRedisTemplate).delete(List.of(ProductRedisKeys.productDetail(10L),
                ProductRedisKeys.productDetail(11L), ProductRedisKeys.productDetail(12L)));
        verify(stringRedisTemplate).delete(ProductRedisKeys.PRODUCT_HOT_LIST);
    }

    @Test
    void shouldUpdateSpuStatusAndClearCaches() {
        when(spuMapper.selectById(100L)).thenReturn(buildSpu());
        when(skuMapper.selectAdminBySpuId(100L)).thenReturn(List.of(buildSku(10L, 100L)));
        AdminSpuStatusUpdateRequest request = new AdminSpuStatusUpdateRequest();
        request.setStatus(0);

        productAdminService.updateSpuStatus(100L, request);

        verify(spuMapper).updateById(any(SpuDO.class));
        verify(stringRedisTemplate).delete(List.of(ProductRedisKeys.productDetail(10L)));
        verify(stringRedisTemplate).delete(ProductRedisKeys.PRODUCT_HOT_LIST);
    }

    @Test
    void shouldDeleteSpuSkuStockAndClearCaches() {
        SkuStockDO stock = buildStock(1000L, 10L, 20, 3);
        when(spuMapper.selectById(100L)).thenReturn(buildSpu());
        when(skuMapper.selectAdminBySpuId(100L)).thenReturn(List.of(buildSku(10L, 100L)));
        when(skuStockMapper.selectBySkuId(10L)).thenReturn(stock);

        productAdminService.deleteSpu(100L);

        verify(skuStockMapper).deleteById(1000L);
        verify(skuMapper).deleteById(10L);
        verify(spuMapper).deleteById(100L);
        verify(stringRedisTemplate).delete(List.of(ProductRedisKeys.productDetail(10L)));
        verify(stringRedisTemplate).delete(ProductRedisKeys.PRODUCT_HOT_LIST);
    }

    private SpuDO buildSpu() {
        SpuDO spu = new SpuDO();
        spu.setId(100L);
        spu.setName("测试商品");
        spu.setCategoryId(1L);
        spu.setBrandId(2L);
        spu.setDescription("描述");
        spu.setMainImage("main.jpg");
        spu.setImages("[\"main-1.jpg\"]");
        spu.setSalesCount(8);
        spu.setStatus(1);
        spu.setIsHot(1);
        spu.setIsNew(0);
        return spu;
    }

    private SkuDO buildSku(Long skuId, Long spuId) {
        SkuDO sku = new SkuDO();
        sku.setId(skuId);
        sku.setSpuId(spuId);
        sku.setName("默认规格");
        sku.setSpecJson("{\"规格\":\"默认\"}");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setCostPrice(new BigDecimal("10.00"));
        sku.setWeight(new BigDecimal("1.00"));
        sku.setImage("sku.jpg");
        sku.setStatus(1);
        return sku;
    }

    private SkuStockDO buildStock(Long id, Long skuId, Integer stock, Integer lockedStock) {
        SkuStockDO stockDO = new SkuStockDO();
        stockDO.setId(id);
        stockDO.setSkuId(skuId);
        stockDO.setStock(stock);
        stockDO.setLockedStock(lockedStock);
        return stockDO;
    }

    private AdminSpuUpdateRequest buildUpdateRequest() {
        AdminSpuUpdateRequest request = new AdminSpuUpdateRequest();
        request.setName("测试商品更新");
        request.setCategoryId(1L);
        request.setBrandId(2L);
        request.setMainImage("main-new.jpg");
        request.setImages(List.of("main-new-1.jpg"));
        request.setStatus(1);
        request.setIsHot(0);
        request.setIsNew(1);

        AdminSpuUpdateRequest.SkuItem existingSku = new AdminSpuUpdateRequest.SkuItem();
        existingSku.setSkuId(10L);
        existingSku.setName("默认规格更新");
        existingSku.setSpec(Map.of("规格", "默认"));
        existingSku.setPrice(new BigDecimal("29.90"));
        existingSku.setStock(30);
        existingSku.setLockedStock(2);

        AdminSpuUpdateRequest.SkuItem newSku = new AdminSpuUpdateRequest.SkuItem();
        newSku.setName("新增规格");
        newSku.setSpec(Map.of("规格", "新增"));
        newSku.setPrice(new BigDecimal("39.90"));
        newSku.setStock(40);
        newSku.setLockedStock(1);
        request.setSkus(List.of(existingSku, newSku));
        return request;
    }
}
