package com.sparkleshop.service.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.service.product.constant.ProductErrorCodes;
import com.sparkleshop.service.product.dto.ProductPageQueryDTO;
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
import com.sparkleshop.service.product.service.ProductQueryService;
import com.sparkleshop.service.product.vo.ProductDetailRespVO;
import com.sparkleshop.service.product.vo.ProductHotRespVO;
import com.sparkleshop.service.product.vo.ProductPageRespVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private static final String PRODUCT_DETAIL_CACHE_KEY_PREFIX = RedisKeys.PRODUCT_CACHE + "detail:";
    private static final String HOT_PRODUCT_CACHE_KEY = RedisKeys.PRODUCT_CACHE + "hot:list";
    private static final Duration PRODUCT_DETAIL_CACHE_TTL = Duration.ofHours(12);
    private static final Duration HOT_PRODUCT_CACHE_TTL = Duration.ofHours(1);
    private static final Duration NULL_PRODUCT_CACHE_TTL = Duration.ofMinutes(5);
    private static final String NULL_CACHE_VALUE = "__NULL__";
    private static final int DEFAULT_HOT_LIMIT = 10;
    private static final int MAX_HOT_LIMIT = 20;

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final SkuStockMapper skuStockMapper;
    private final BrandMapper brandMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ProductPageRespVO getProductPage(ProductPageQueryDTO queryDTO) {
        Set<Long> categoryIds = resolveCategoryIds(queryDTO.getCategoryId());
        if (queryDTO.getCategoryId() != null && categoryIds.isEmpty()) {
            return emptyPage(queryDTO);
        }

        List<SpuDO> spus = spuMapper.selectActiveList(categoryIds, queryDTO.getBrandId(), queryDTO.getKeyword());
        if (spus.isEmpty()) {
            return emptyPage(queryDTO);
        }

        Map<Long, List<SkuDO>> skuMap = groupSkuBySpuId(skuMapper.selectBySpuIds(extractIds(spus, SpuDO::getId)));
        Map<Long, SkuStockDO> stockMap = groupStockBySkuId(skuStockMapper.selectBySkuIds(extractIds(
                skuMap.values().stream().flatMap(Collection::stream).toList(), SkuDO::getId)));

        List<DisplayProduct> displayProducts = buildDisplayProducts(spus, skuMap, stockMap, false);
        displayProducts.sort(resolveComparator(queryDTO.getSortType()));

        int pageNo = queryDTO.getPageNo() == null ? 1 : queryDTO.getPageNo();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(displayProducts.size(), fromIndex + pageSize);

        ProductPageRespVO response = new ProductPageRespVO();
        response.setTotal(displayProducts.size());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        if (fromIndex >= displayProducts.size()) {
            response.setList(Collections.emptyList());
            return response;
        }
        response.setList(displayProducts.subList(fromIndex, toIndex).stream()
                .map(this::toPageItem)
                .toList());
        return response;
    }

    @Override
    public ProductDetailRespVO getProductDetail(Long skuId) {
        String cacheKey = PRODUCT_DETAIL_CACHE_KEY_PREFIX + skuId;
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            if (NULL_CACHE_VALUE.equals(cachedValue)) {
                throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不存在");
            }
            try {
                return objectMapper.readValue(cachedValue, ProductDetailRespVO.class);
            } catch (JsonProcessingException ignored) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        SkuDO sku = skuMapper.selectById(skuId);
        if (sku == null) {
            cacheNullProduct(cacheKey);
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不存在");
        }
        if (!Objects.equals(sku.getStatus(), 1)) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不可用");
        }

        SpuDO spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) {
            cacheNullProduct(cacheKey);
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不存在");
        }
        if (!Objects.equals(spu.getStatus(), 1)) {
            throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "商品不可用");
        }

        BrandDO brand = brandMapper.selectById(spu.getBrandId());
        SkuStockDO stock = skuStockMapper.selectBySkuId(skuId);
        List<SkuDO> skuOptions = skuMapper.selectBySpuId(spu.getId());

        ProductDetailRespVO response = new ProductDetailRespVO();
        response.setSkuId(sku.getId());
        response.setSpuId(spu.getId());
        response.setName(spu.getName());
        response.setPrice(sku.getPrice());
        response.setStock(getAvailableStock(stock));
        response.setMainImage(firstNonBlank(sku.getImage(), spu.getMainImage()));
        response.setImages(parseJsonArray(spu.getImages()));
        response.setSpec(parseJsonMap(sku.getSpecJson()));
        response.setStatus(spu.getStatus());
        response.setDescription(spu.getDescription());
        response.setCategoryId(spu.getCategoryId());
        response.setBrandId(spu.getBrandId());
        response.setBrandName(brand == null ? null : brand.getName());
        response.setSalesCount(defaultInt(spu.getSalesCount()));
        response.setIsHot(defaultInt(spu.getIsHot()));
        response.setIsNew(defaultInt(spu.getIsNew()));
        response.setSkuOptions(skuOptions.stream().map(this::toSkuOption).toList());

        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(response), PRODUCT_DETAIL_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return response;
    }

    @Override
    public List<ProductHotRespVO> getHotProducts(Integer limit) {
        int actualLimit = normalizeHotLimit(limit);
        String cachedValue = stringRedisTemplate.opsForValue().get(HOT_PRODUCT_CACHE_KEY);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                List<ProductHotRespVO> cachedList = objectMapper.readValue(cachedValue, new TypeReference<List<ProductHotRespVO>>() {
                });
                return cachedList.subList(0, Math.min(actualLimit, cachedList.size()));
            } catch (JsonProcessingException ignored) {
                stringRedisTemplate.delete(HOT_PRODUCT_CACHE_KEY);
            }
        }

        List<SpuDO> spus = spuMapper.selectActiveList(null, null, null);
        if (spus.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<SkuDO>> skuMap = groupSkuBySpuId(skuMapper.selectBySpuIds(extractIds(spus, SpuDO::getId)));
        Map<Long, SkuStockDO> stockMap = groupStockBySkuId(skuStockMapper.selectBySkuIds(extractIds(
                skuMap.values().stream().flatMap(Collection::stream).toList(), SkuDO::getId)));

        List<ProductHotRespVO> fullList = buildDisplayProducts(spus, skuMap, stockMap, true).stream()
                .sorted(Comparator.comparing(DisplayProduct::getIsHot, Comparator.reverseOrder())
                        .thenComparing(DisplayProduct::getSalesCount, Comparator.reverseOrder())
                        .thenComparing(DisplayProduct::getSpuId, Comparator.reverseOrder()))
                .limit(MAX_HOT_LIMIT)
                .map(this::toHotResp)
                .toList();

        try {
            stringRedisTemplate.opsForValue().set(HOT_PRODUCT_CACHE_KEY,
                    objectMapper.writeValueAsString(fullList), HOT_PRODUCT_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return fullList.subList(0, Math.min(actualLimit, fullList.size()));
    }

    private ProductPageRespVO emptyPage(ProductPageQueryDTO queryDTO) {
        ProductPageRespVO response = new ProductPageRespVO();
        response.setList(Collections.emptyList());
        response.setTotal(0);
        response.setPageNo(queryDTO.getPageNo() == null ? 1 : queryDTO.getPageNo());
        response.setPageSize(queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize());
        return response;
    }

    private Set<Long> resolveCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<CategoryDO> categories = categoryMapper.selectEnabledList();
        if (categories.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        boolean found = false;
        for (CategoryDO category : categories) {
            if (Objects.equals(category.getId(), categoryId)) {
                found = true;
            }
            childrenMap.computeIfAbsent(category.getParentId(), key -> new ArrayList<>()).add(category.getId());
        }
        if (!found) {
            return Collections.emptySet();
        }

        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (result.add(current)) {
                queue.addAll(childrenMap.getOrDefault(current, Collections.emptyList()));
            }
        }
        return result;
    }

    private Map<Long, List<SkuDO>> groupSkuBySpuId(List<SkuDO> skus) {
        return skus.stream().collect(Collectors.groupingBy(SkuDO::getSpuId));
    }

    private Map<Long, SkuStockDO> groupStockBySkuId(List<SkuStockDO> stocks) {
        return stocks.stream().collect(Collectors.toMap(SkuStockDO::getSkuId, Function.identity(), (left, right) -> left));
    }

    private <T> List<Long> extractIds(List<T> dataList, Function<T, Long> idGetter) {
        return dataList.stream().map(idGetter).filter(Objects::nonNull).toList();
    }

    private List<DisplayProduct> buildDisplayProducts(List<SpuDO> spus, Map<Long, List<SkuDO>> skuMap,
                                                      Map<Long, SkuStockDO> stockMap, boolean requireInStock) {
        List<DisplayProduct> result = new ArrayList<>();
        for (SpuDO spu : spus) {
            List<SkuDO> skuList = skuMap.getOrDefault(spu.getId(), Collections.emptyList());
            SkuDO displaySku = skuList.stream()
                    .filter(sku -> !requireInStock || getAvailableStock(stockMap.get(sku.getId())) > 0)
                    .min(Comparator.comparing(SkuDO::getPrice).thenComparing(SkuDO::getId))
                    .orElse(null);
            if (displaySku == null) {
                continue;
            }

            DisplayProduct displayProduct = new DisplayProduct();
            displayProduct.setSkuId(displaySku.getId());
            displayProduct.setSpuId(spu.getId());
            displayProduct.setName(spu.getName());
            displayProduct.setPrice(displaySku.getPrice());
            displayProduct.setMainImage(firstNonBlank(displaySku.getImage(), spu.getMainImage()));
            displayProduct.setSalesCount(defaultInt(spu.getSalesCount()));
            displayProduct.setStockStatus(getAvailableStock(stockMap.get(displaySku.getId())) > 0 ? 1 : 0);
            displayProduct.setSpecSummary(buildSpecSummary(displaySku.getSpecJson()));
            displayProduct.setIsHot(defaultInt(spu.getIsHot()));
            displayProduct.setIsNew(defaultInt(spu.getIsNew()));
            result.add(displayProduct);
        }
        return result;
    }

    private Comparator<DisplayProduct> resolveComparator(String sortType) {
        if ("sales".equalsIgnoreCase(sortType)) {
            return Comparator.comparing(DisplayProduct::getSalesCount, Comparator.reverseOrder())
                    .thenComparing(DisplayProduct::getSpuId, Comparator.reverseOrder());
        }
        if ("priceAsc".equalsIgnoreCase(sortType)) {
            return Comparator.comparing(DisplayProduct::getPrice)
                    .thenComparing(DisplayProduct::getSpuId);
        }
        if ("priceDesc".equalsIgnoreCase(sortType)) {
            return Comparator.comparing(DisplayProduct::getPrice, Comparator.reverseOrder())
                    .thenComparing(DisplayProduct::getSpuId, Comparator.reverseOrder());
        }
        return Comparator.comparing(DisplayProduct::getIsHot, Comparator.reverseOrder())
                .thenComparing(DisplayProduct::getIsNew, Comparator.reverseOrder())
                .thenComparing(DisplayProduct::getSalesCount, Comparator.reverseOrder())
                .thenComparing(DisplayProduct::getSpuId, Comparator.reverseOrder());
    }

    private ProductPageRespVO.Item toPageItem(DisplayProduct product) {
        ProductPageRespVO.Item item = new ProductPageRespVO.Item();
        item.setSkuId(product.getSkuId());
        item.setSpuId(product.getSpuId());
        item.setName(product.getName());
        item.setPrice(product.getPrice());
        item.setMainImage(product.getMainImage());
        item.setSalesCount(product.getSalesCount());
        item.setStockStatus(product.getStockStatus());
        item.setSpecSummary(product.getSpecSummary());
        item.setIsHot(product.getIsHot());
        item.setIsNew(product.getIsNew());
        return item;
    }

    private ProductHotRespVO toHotResp(DisplayProduct product) {
        ProductHotRespVO response = new ProductHotRespVO();
        response.setSkuId(product.getSkuId());
        response.setSpuId(product.getSpuId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setMainImage(product.getMainImage());
        response.setSalesCount(product.getSalesCount());
        if (product.getIsHot() == 1) {
            response.setTag("热销");
        } else if (product.getIsNew() == 1) {
            response.setTag("新品");
        } else {
            response.setTag("推荐");
        }
        return response;
    }

    private ProductDetailRespVO.SkuOption toSkuOption(SkuDO sku) {
        ProductDetailRespVO.SkuOption option = new ProductDetailRespVO.SkuOption();
        option.setSkuId(sku.getId());
        option.setName(sku.getName());
        option.setPrice(sku.getPrice());
        option.setImage(sku.getImage());
        return option;
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

    private String buildSpecSummary(String specJson) {
        Map<String, Object> specMap = parseJsonMap(specJson);
        if (specMap.isEmpty()) {
            return null;
        }
        return specMap.values().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    private int getAvailableStock(SkuStockDO stock) {
        if (stock == null) {
            return 0;
        }
        return Math.max(0, defaultInt(stock.getStock()) - defaultInt(stock.getLockedStock()));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : StrUtil.trimToNull(second);
    }

    private int normalizeHotLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_HOT_LIMIT;
        }
        return Math.min(limit, MAX_HOT_LIMIT);
    }

    private void cacheNullProduct(String cacheKey) {
        stringRedisTemplate.opsForValue().set(cacheKey, NULL_CACHE_VALUE, NULL_PRODUCT_CACHE_TTL);
    }

    @Data
    private static class DisplayProduct {

        private Long skuId;

        private Long spuId;

        private String name;

        private BigDecimal price;

        private String mainImage;

        private Integer salesCount;

        private Integer stockStatus;

        private String specSummary;

        private Integer isHot;

        private Integer isNew;
    }
}
