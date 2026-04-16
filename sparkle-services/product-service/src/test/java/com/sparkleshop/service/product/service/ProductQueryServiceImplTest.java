package com.sparkleshop.service.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.service.product.dto.ProductPageQueryDTO;
import com.sparkleshop.service.product.entity.SkuDO;
import com.sparkleshop.service.product.entity.SkuStockDO;
import com.sparkleshop.service.product.entity.SpuDO;
import com.sparkleshop.service.product.mapper.BrandMapper;
import com.sparkleshop.service.product.mapper.CategoryMapper;
import com.sparkleshop.service.product.mapper.SkuMapper;
import com.sparkleshop.service.product.mapper.SkuStockMapper;
import com.sparkleshop.service.product.mapper.SpuMapper;
import com.sparkleshop.service.product.service.impl.ProductQueryServiceImpl;
import com.sparkleshop.service.product.vo.ProductDetailRespVO;
import com.sparkleshop.service.product.vo.ProductHotRespVO;
import com.sparkleshop.service.product.vo.ProductPageRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

    @Mock
    private SpuMapper spuMapper;
    @Mock
    private SkuMapper skuMapper;
    @Mock
    private SkuStockMapper skuStockMapper;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ProductQueryServiceImpl productQueryService;

    @BeforeEach
    void setUp() {
        productQueryService = new ProductQueryServiceImpl(
                spuMapper, skuMapper, skuStockMapper, brandMapper, categoryMapper, stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void shouldReturnProductPageSortedByPriceAsc() {
        SpuDO apple = buildSpu(1L, "苹果", 100, 1, 0);
        SpuDO mango = buildSpu(2L, "芒果", 50, 0, 1);
        when(spuMapper.selectActiveList(null, null, null)).thenReturn(List.of(apple, mango));
        when(skuMapper.selectBySpuIds(anyCollection())).thenReturn(List.of(
                buildSku(11L, 1L, "苹果 5斤", "{\"规格\":\"5斤装\"}", new BigDecimal("39.90"), "apple.jpg"),
                buildSku(12L, 2L, "芒果 3斤", "{\"规格\":\"3斤装\"}", new BigDecimal("29.90"), "mango.jpg")
        ));
        when(skuStockMapper.selectBySkuIds(anyCollection())).thenReturn(List.of(
                buildStock(11L, 100, 0),
                buildStock(12L, 50, 5)
        ));

        ProductPageQueryDTO queryDTO = new ProductPageQueryDTO();
        queryDTO.setSortType("priceAsc");
        ProductPageRespVO response = productQueryService.getProductPage(queryDTO);

        assertEquals(2, response.getTotal());
        assertEquals(12L, response.getList().get(0).getSkuId());
        assertEquals("3斤装", response.getList().get(0).getSpecSummary());
    }

    @Test
    void shouldCacheNullWhenProductNotFound() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(skuMapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> productQueryService.getProductDetail(1L));

        verify(valueOperations).set(anyString(), anyString(), any());
    }

    @Test
    void shouldReturnHotProductsWithStockOnly() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        SpuDO apple = buildSpu(1L, "苹果", 300, 1, 0);
        SpuDO mango = buildSpu(2L, "芒果", 100, 1, 1);
        when(spuMapper.selectActiveList(null, null, null)).thenReturn(List.of(apple, mango));
        when(skuMapper.selectBySpuIds(anyCollection())).thenReturn(List.of(
                buildSku(11L, 1L, "苹果 5斤", "{\"规格\":\"5斤装\"}", new BigDecimal("39.90"), "apple.jpg"),
                buildSku(12L, 2L, "芒果 3斤", "{\"规格\":\"3斤装\"}", new BigDecimal("29.90"), "mango.jpg")
        ));
        when(skuStockMapper.selectBySkuIds(anyCollection())).thenReturn(List.of(
                buildStock(11L, 10, 0),
                buildStock(12L, 5, 5)
        ));

        List<ProductHotRespVO> response = productQueryService.getHotProducts(10);

        assertEquals(1, response.size());
        assertEquals(11L, response.get(0).getSkuId());
    }

    @Test
    void shouldReadProductDetailFromCache() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ProductDetailRespVO cached = new ProductDetailRespVO();
        cached.setSkuId(9L);
        cached.setName("缓存商品");
        when(valueOperations.get(anyString())).thenReturn(new ObjectMapper().writeValueAsString(cached));

        ProductDetailRespVO response = productQueryService.getProductDetail(9L);

        assertEquals(9L, response.getSkuId());
        assertEquals("缓存商品", response.getName());
    }

    private SpuDO buildSpu(Long id, String name, int salesCount, int isHot, int isNew) {
        SpuDO spu = new SpuDO();
        spu.setId(id);
        spu.setName(name);
        spu.setSalesCount(salesCount);
        spu.setIsHot(isHot);
        spu.setIsNew(isNew);
        spu.setStatus(1);
        return spu;
    }

    private SkuDO buildSku(Long id, Long spuId, String name, String specJson, BigDecimal price, String image) {
        SkuDO sku = new SkuDO();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setName(name);
        sku.setSpecJson(specJson);
        sku.setPrice(price);
        sku.setImage(image);
        sku.setStatus(1);
        return sku;
    }

    private SkuStockDO buildStock(Long skuId, int stock, int lockedStock) {
        SkuStockDO stockDO = new SkuStockDO();
        stockDO.setSkuId(skuId);
        stockDO.setStock(stock);
        stockDO.setLockedStock(lockedStock);
        return stockDO;
    }
}
