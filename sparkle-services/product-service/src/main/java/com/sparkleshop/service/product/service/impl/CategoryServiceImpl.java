package com.sparkleshop.service.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.service.product.constant.ProductErrorCodes;
import com.sparkleshop.service.product.dto.admin.AdminCategoryCreateRequest;
import com.sparkleshop.service.product.entity.CategoryDO;
import com.sparkleshop.service.product.mapper.CategoryMapper;
import com.sparkleshop.service.product.service.CategoryService;
import com.sparkleshop.service.product.vo.AdminCategoryRespVO;
import com.sparkleshop.service.product.vo.ProductCategoryTreeRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_TREE_CACHE_KEY = RedisKeys.PRODUCT_CACHE + "category:tree";
    private static final Duration CATEGORY_TREE_CACHE_TTL = Duration.ofDays(1);

    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<ProductCategoryTreeRespVO> getCategoryTree() {
        String cachedValue = stringRedisTemplate.opsForValue().get(CATEGORY_TREE_CACHE_KEY);
        if (StrUtil.isNotBlank(cachedValue)) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<List<ProductCategoryTreeRespVO>>() {
                });
            } catch (JsonProcessingException ignored) {
                stringRedisTemplate.delete(CATEGORY_TREE_CACHE_KEY);
            }
        }

        List<ProductCategoryTreeRespVO> tree = buildTree(categoryMapper.selectEnabledList());
        try {
            stringRedisTemplate.opsForValue().set(CATEGORY_TREE_CACHE_KEY,
                    objectMapper.writeValueAsString(tree), CATEGORY_TREE_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // 缓存失败不影响主流程
        }
        return tree;
    }

    @Override
    public List<AdminCategoryRespVO> getAdminCategoryList() {
        return categoryMapper.selectAdminList().stream()
                .map(this::toAdminCategoryResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(AdminCategoryCreateRequest request) {
        long parentId = request.getParentId() == null ? 0L : request.getParentId();
        int level = 1;
        if (parentId > 0) {
            CategoryDO parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ProductErrorCodes.RESOURCE_NOT_FOUND, "父分类不存在");
            }
            level = parent.getLevel() + 1;
            if (level > 3) {
                throw new BusinessException(ProductErrorCodes.INVALID_REQUEST, "商品分类最多支持三级");
            }
        }

        CategoryDO category = new CategoryDO();
        category.setName(StrUtil.trim(request.getName()));
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSort(request.getSort());
        category.setStatus(request.getStatus());
        category.setIcon(StrUtil.trimToNull(request.getIcon()));
        categoryMapper.insert(category);
        stringRedisTemplate.delete(CATEGORY_TREE_CACHE_KEY);
        return category.getId();
    }

    private List<ProductCategoryTreeRespVO> buildTree(List<CategoryDO> categories) {
        Map<Long, ProductCategoryTreeRespVO> nodeMap = new LinkedHashMap<>();
        for (CategoryDO category : categories) {
            ProductCategoryTreeRespVO node = new ProductCategoryTreeRespVO();
            node.setId(category.getId());
            node.setName(category.getName());
            node.setParentId(category.getParentId());
            node.setLevel(category.getLevel());
            node.setSort(category.getSort());
            node.setIcon(category.getIcon());
            nodeMap.put(node.getId(), node);
        }

        List<ProductCategoryTreeRespVO> roots = new ArrayList<>();
        for (ProductCategoryTreeRespVO node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
                continue;
            }
            ProductCategoryTreeRespVO parent = nodeMap.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private AdminCategoryRespVO toAdminCategoryResp(CategoryDO category) {
        AdminCategoryRespVO response = new AdminCategoryRespVO();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setLevel(category.getLevel());
        response.setSort(category.getSort());
        response.setStatus(category.getStatus());
        response.setIcon(category.getIcon());
        return response;
    }
}
