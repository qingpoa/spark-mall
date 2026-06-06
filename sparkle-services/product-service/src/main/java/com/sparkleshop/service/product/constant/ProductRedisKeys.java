package com.sparkleshop.service.product.constant;

import java.time.Duration;

public final class ProductRedisKeys {

    public static final String PRODUCT_CACHE = "sparkle:product:";
    public static final String PRODUCT_CATEGORY_TREE = PRODUCT_CACHE + "category:tree";
    public static final String PRODUCT_DETAIL_PREFIX = PRODUCT_CACHE + "detail:";
    public static final String PRODUCT_PAGE = PRODUCT_CACHE + "page:";
    public static final String PRODUCT_HOT_LIST = PRODUCT_CACHE + "hot:list";
    public static final Duration PRODUCT_CATEGORY_TREE_TTL = Duration.ofDays(1);
    public static final Duration PRODUCT_DETAIL_TTL = Duration.ofHours(12);
    public static final Duration PRODUCT_PAGE_TTL = Duration.ofMinutes(2);
    public static final Duration PRODUCT_HOT_LIST_TTL = Duration.ofHours(1);
    public static final Duration PRODUCT_NULL_TTL = Duration.ofMinutes(5);

    private ProductRedisKeys() {
    }

    public static String productDetail(Long skuId) {
        return PRODUCT_DETAIL_PREFIX + skuId;
    }
}
