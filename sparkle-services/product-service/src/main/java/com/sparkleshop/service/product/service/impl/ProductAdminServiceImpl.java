package com.sparkleshop.service.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.service.product.constant.ProductErrorCodes;
import com.sparkleshop.service.product.constant.ProductRedisKeys;
import com.sparkleshop.service.product.dto.admin.AdminSpuCreateRequest;
import com.sparkleshop.service.product.dto.admin.AdminSpuPageQueryDTO;
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
import com.sparkleshop.service.product.service.ProductAdminService;
import com.sparkleshop.service.product.vo.AdminSpuDetailRespVO;
import com.sparkleshop.service.product.vo.AdminSpuPageRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

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
        validateCategoryAndBrand(request.getCategoryId(), request.getBrandId());
        if (request.getSkus() == null || request.getSkus().isEmpty()) {
            throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "至少需要一个 SKU");
        }

        SpuDO spu = new SpuDO();
        fillSpu(spu, request.getName(), request.getCategoryId(), request.getBrandId(), request.getDescription(),
                request.getMainImage(), request.getImages(), request.getStatus(), request.getIsHot(), request.getIsNew());
        spu.setSalesCount(0);
        spuMapper.insert(spu);

        for (AdminSpuCreateRequest.SkuItem skuItem : request.getSkus()) {
            SkuDO sku = new SkuDO();
            fillSku(sku, spu.getId(), skuItem.getName(), skuItem.getSpec(), skuItem.getPrice(), skuItem.getCostPrice(),
                    skuItem.getWeight(), firstNonBlank(skuItem.getImage(), request.getMainImage()), skuItem.getStatus());
            skuMapper.insert(sku);
            createOrUpdateStock(sku.getId(), skuItem.getStock(), skuItem.getLockedStock());
        }

        clearHotCache();
        return spu.getId();
    }

    @Override
    public AdminSpuDetailRespVO getSpuDetail(Long spuId) {
        SpuDO spu = getExistingSpu(spuId);
        List<SkuDO> skus = skuMapper.selectAdminBySpuId(spuId);
        Map<Long, SkuStockDO> stockMap = stockMapBySkuId(skuStockMapper.selectBySkuIds(extractSkuIds(skus)));

        AdminSpuDetailRespVO response = new AdminSpuDetailRespVO();
        response.setId(spu.getId());
        response.setName(spu.getName());
        response.setCategoryId(spu.getCategoryId());
        response.setBrandId(spu.getBrandId());
        response.setDescription(spu.getDescription());
        response.setMainImage(spu.getMainImage());
        response.setImages(parseJsonArray(spu.getImages()));
        response.setSalesCount(defaultInt(spu.getSalesCount()));
        response.setStatus(spu.getStatus());
        response.setIsHot(spu.getIsHot());
        response.setIsNew(spu.getIsNew());
        response.setSkus(skus.stream()
                .map(sku -> toDetailSkuItem(sku, stockMap.get(sku.getId())))
                .toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpu(Long spuId, AdminSpuUpdateRequest request) {
        SpuDO existingSpu = getExistingSpu(spuId);
        validateCategoryAndBrand(request.getCategoryId(), request.getBrandId());
        if (request.getSkus() == null || request.getSkus().isEmpty()) {
            throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "至少需要一个 SKU");
        }

        SpuDO spu = new SpuDO();
        spu.setId(spuId);
        fillSpu(spu, request.getName(), request.getCategoryId(), request.getBrandId(), request.getDescription(),
                request.getMainImage(), request.getImages(), request.getStatus(), request.getIsHot(), request.getIsNew());
        spu.setSalesCount(existingSpu.getSalesCount());
        spuMapper.updateById(spu);

        List<SkuDO> existingSkus = skuMapper.selectAdminBySpuId(spuId);
        Map<Long, SkuDO> existingSkuMap = existingSkus.stream()
                .collect(Collectors.toMap(SkuDO::getId, Function.identity(), (left, right) -> left));
        Set<Long> submittedSkuIds = new HashSet<>();
        List<Long> affectedSkuIds = new ArrayList<>(extractSkuIds(existingSkus));

        for (AdminSpuUpdateRequest.SkuItem skuItem : request.getSkus()) {
            Long submittedSkuId = skuItem.getSkuId();
            if (submittedSkuId == null) {
                SkuDO sku = new SkuDO();
                fillSku(sku, spuId, skuItem.getName(), skuItem.getSpec(), skuItem.getPrice(), skuItem.getCostPrice(),
                        skuItem.getWeight(), firstNonBlank(skuItem.getImage(), request.getMainImage()), skuItem.getStatus());
                skuMapper.insert(sku);
                createOrUpdateStock(sku.getId(), skuItem.getStock(), skuItem.getLockedStock());
                affectedSkuIds.add(sku.getId());
                continue;
            }

            SkuDO existingSku = existingSkuMap.get(submittedSkuId);
            if (existingSku == null) {
                throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "SKU 不存在或不属于当前商品");
            }
            submittedSkuIds.add(submittedSkuId);

            SkuDO sku = new SkuDO();
            sku.setId(submittedSkuId);
            fillSku(sku, spuId, skuItem.getName(), skuItem.getSpec(), skuItem.getPrice(), skuItem.getCostPrice(),
                    skuItem.getWeight(), firstNonBlank(skuItem.getImage(), request.getMainImage()), skuItem.getStatus());
            skuMapper.updateById(sku);
            createOrUpdateStock(submittedSkuId, skuItem.getStock(), skuItem.getLockedStock());
        }

        for (SkuDO existingSku : existingSkus) {
            if (!submittedSkuIds.contains(existingSku.getId())) {
                deleteSkuAndStock(existingSku.getId());
            }
        }

        clearProductCaches(affectedSkuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpuStatus(Long spuId, AdminSpuStatusUpdateRequest request) {
        getExistingSpu(spuId);
        SpuDO spu = new SpuDO();
        spu.setId(spuId);
        spu.setStatus(request.getStatus());
        spuMapper.updateById(spu);
        clearProductCaches(extractSkuIds(skuMapper.selectAdminBySpuId(spuId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpu(Long spuId) {
        getExistingSpu(spuId);
        List<Long> skuIds = extractSkuIds(skuMapper.selectAdminBySpuId(spuId));
        for (Long skuId : skuIds) {
            deleteSkuAndStock(skuId);
        }
        spuMapper.deleteById(spuId);
        clearProductCaches(skuIds);
    }

    private void validateCategoryAndBrand(Long categoryId, Long brandId) {
        CategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品分类不存在");
        }
        BrandDO brand = brandMapper.selectById(brandId);
        if (brand == null) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品品牌不存在");
        }
    }

    private SpuDO getExistingSpu(Long spuId) {
        SpuDO spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不存在");
        }
        return spu;
    }

    private void fillSpu(SpuDO spu, String name, Long categoryId, Long brandId, String description,
                         String mainImage, List<String> images, Integer status, Integer isHot, Integer isNew) {
        spu.setName(StrUtil.trim(name));
        spu.setCategoryId(categoryId);
        spu.setBrandId(brandId);
        spu.setDescription(StrUtil.trimToNull(description));
        spu.setMainImage(StrUtil.trimToNull(mainImage));
        spu.setImages(serializeJson(images == null ? Collections.emptyList() : images));
        spu.setStatus(status);
        spu.setIsHot(isHot);
        spu.setIsNew(isNew);
    }

    private void fillSku(SkuDO sku, Long spuId, String name, Map<String, Object> spec, BigDecimal price,
                         BigDecimal costPrice, BigDecimal weight, String image, Integer status) {
        sku.setSpuId(spuId);
        sku.setName(StrUtil.trim(name));
        sku.setSpecJson(serializeJson(spec == null ? Collections.emptyMap() : spec));
        sku.setPrice(price);
        sku.setCostPrice(defaultDecimal(costPrice));
        sku.setWeight(weight);
        sku.setImage(StrUtil.trimToNull(image));
        sku.setStatus(status);
    }

    private void createOrUpdateStock(Long skuId, Integer stockValue, Integer lockedStockValue) {
        int stock = Math.max(0, defaultInt(stockValue));
        int lockedStock = Math.max(0, Math.min(stock, defaultInt(lockedStockValue)));
        SkuStockDO stockDO = skuStockMapper.selectBySkuId(skuId);
        if (stockDO == null) {
            stockDO = new SkuStockDO();
            stockDO.setSkuId(skuId);
            stockDO.setStock(stock);
            stockDO.setLockedStock(lockedStock);
            skuStockMapper.insert(stockDO);
            return;
        }
        stockDO.setStock(stock);
        stockDO.setLockedStock(lockedStock);
        skuStockMapper.updateById(stockDO);
    }

    private void deleteSkuAndStock(Long skuId) {
        SkuStockDO stock = skuStockMapper.selectBySkuId(skuId);
        if (stock != null) {
            skuStockMapper.deleteById(stock.getId());
        }
        skuMapper.deleteById(skuId);
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

    private AdminSpuDetailRespVO.SkuItem toDetailSkuItem(SkuDO sku, SkuStockDO stock) {
        AdminSpuDetailRespVO.SkuItem item = new AdminSpuDetailRespVO.SkuItem();
        item.setSkuId(sku.getId());
        item.setName(sku.getName());
        item.setSpec(parseJsonMap(sku.getSpecJson()));
        item.setPrice(sku.getPrice());
        item.setCostPrice(sku.getCostPrice());
        item.setWeight(sku.getWeight());
        item.setImage(sku.getImage());
        item.setStatus(sku.getStatus());
        item.setStock(stock == null ? 0 : defaultInt(stock.getStock()));
        item.setLockedStock(stock == null ? 0 : defaultInt(stock.getLockedStock()));
        item.setAvailableStock(Math.max(0, item.getStock() - item.getLockedStock()));
        return item;
    }

    private List<Long> extractSkuIds(Collection<SkuDO> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        return skus.stream().map(SkuDO::getId).filter(Objects::nonNull).distinct().toList();
    }

    private Map<Long, SkuStockDO> stockMapBySkuId(List<SkuStockDO> stocks) {
        return stocks.stream().collect(Collectors.toMap(SkuStockDO::getSkuId, Function.identity(), (left, right) -> left));
    }

    private List<String> parseJsonArray(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ignored) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException ignored) {
            return Collections.emptyMap();
        }
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "商品规格格式不合法");
        }
    }

    private void clearProductCaches(Collection<Long> skuIds) {
        if (skuIds != null && !skuIds.isEmpty()) {
            List<String> detailKeys = skuIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(ProductRedisKeys::productDetail)
                    .toList();
            if (!detailKeys.isEmpty()) {
                stringRedisTemplate.delete(detailKeys);
            }
        }
        clearHotCache();
        clearPageCache();
    }

    private void clearPageCache() {
        Set<String> pageKeys = stringRedisTemplate.keys(ProductRedisKeys.PRODUCT_PAGE + "*");
        if (pageKeys != null && !pageKeys.isEmpty()) {
            stringRedisTemplate.delete(pageKeys);
        }
    }

    private void clearHotCache() {
        stringRedisTemplate.delete(ProductRedisKeys.PRODUCT_HOT_LIST);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : StrUtil.trimToNull(second);
    }
}
