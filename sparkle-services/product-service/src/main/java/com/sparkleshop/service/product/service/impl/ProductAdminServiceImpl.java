package com.sparkleshop.service.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.service.product.constant.ProductErrorCodes;
import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuPageQueryDTO;
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
import com.sparkleshop.service.product.service.ProductAdminService;
import com.sparkleshop.service.product.vo.AdminSpuPageRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

    private static final String HOT_PRODUCT_CACHE_KEY = RedisKeys.PRODUCT_CACHE + "hot:list";

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final SkuStockMapper skuStockMapper;
    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AdminSpuPageRespVO getAdminSpuPage(AdminSpuPageQueryDTO queryDTO) {
        Page<SpuDO> page = spuMapper.selectAdminPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryDTO.getKeyword(), queryDTO.getCategoryId(), queryDTO.getBrandId(), queryDTO.getStatus());

        List<Long> spuIds = page.getRecords().stream().map(SpuDO::getId).toList();
        Map<Long, Integer> skuCountMap = skuMapper.selectAdminBySpuIds(spuIds).stream()
                .collect(Collectors.groupingBy(SkuDO::getSpuId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        AdminSpuPageRespVO response = new AdminSpuPageRespVO();
        response.setTotal(page.getTotal());
        response.setPageNo(page.getCurrent());
        response.setPageSize(page.getSize());
        response.setList(page.getRecords().stream()
                .map(spu -> toAdminSpuItem(spu, skuCountMap.getOrDefault(spu.getId(), 0)))
                .toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSpu(AdminSpuCreateRequest request) {
        CategoryDO category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品分类不存在");
        }
        BrandDO brand = brandMapper.selectById(request.getBrandId());
        if (brand == null) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品品牌不存在");
        }
        if (request.getSkus() == null || request.getSkus().isEmpty()) {
            throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "至少需要一个 SKU");
        }

        SpuDO spu = new SpuDO();
        spu.setName(StrUtil.trim(request.getName()));
        spu.setCategoryId(request.getCategoryId());
        spu.setBrandId(request.getBrandId());
        spu.setDescription(StrUtil.trimToNull(request.getDescription()));
        spu.setMainImage(StrUtil.trimToNull(request.getMainImage()));
        spu.setImages(serializeJson(request.getImages() == null ? Collections.emptyList() : request.getImages()));
        spu.setSalesCount(0);
        spu.setStatus(request.getStatus());
        spu.setIsHot(request.getIsHot());
        spu.setIsNew(request.getIsNew());
        spuMapper.insert(spu);

        for (AdminSpuCreateRequest.SkuItem skuItem : request.getSkus()) {
            SkuDO sku = new SkuDO();
            sku.setSpuId(spu.getId());
            sku.setName(StrUtil.trim(skuItem.getName()));
            sku.setSpecJson(serializeJson(skuItem.getSpec() == null ? Collections.emptyMap() : skuItem.getSpec()));
            sku.setPrice(skuItem.getPrice());
            sku.setCostPrice(defaultDecimal(skuItem.getCostPrice()));
            sku.setWeight(skuItem.getWeight());
            sku.setImage(StrUtil.trimToNull(firstNonBlank(skuItem.getImage(), request.getMainImage())));
            sku.setStatus(skuItem.getStatus());
            skuMapper.insert(sku);

            int stock = Math.max(0, skuItem.getStock());
            int lockedStock = Math.max(0, Math.min(stock, skuItem.getLockedStock()));
            SkuStockDO stockDO = new SkuStockDO();
            stockDO.setSkuId(sku.getId());
            stockDO.setStock(stock);
            stockDO.setLockedStock(lockedStock);
            skuStockMapper.insert(stockDO);
        }

        stringRedisTemplate.delete(HOT_PRODUCT_CACHE_KEY);
        return spu.getId();
    }

    private AdminSpuPageRespVO.Item toAdminSpuItem(SpuDO spu, Integer skuCount) {
        AdminSpuPageRespVO.Item item = new AdminSpuPageRespVO.Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setCategoryId(spu.getCategoryId());
        item.setBrandId(spu.getBrandId());
        item.setMainImage(spu.getMainImage());
        item.setSalesCount(spu.getSalesCount());
        item.setStatus(spu.getStatus());
        item.setIsHot(spu.getIsHot());
        item.setIsNew(spu.getIsNew());
        item.setSkuCount(skuCount);
        return item;
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "商品规格格式不合法");
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : StrUtil.trimToNull(second);
    }
}
