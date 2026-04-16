package com.sparkleshop.service.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.service.product.entity.CategoryDO;
import com.sparkleshop.service.product.mapper.CategoryMapper;
import com.sparkleshop.service.product.service.impl.CategoryServiceImpl;
import com.sparkleshop.service.product.vo.ProductCategoryTreeRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryMapper, stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void shouldBuildTreeFromFlatCategories() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(categoryMapper.selectEnabledList()).thenReturn(List.of(
                buildCategory(1L, "水果", 0L, 1),
                buildCategory(2L, "苹果", 1L, 2),
                buildCategory(3L, "红富士", 2L, 3)
        ));

        List<ProductCategoryTreeRespVO> response = categoryService.getCategoryTree();

        assertEquals(1, response.size());
        assertEquals(1, response.get(0).getChildren().size());
        assertEquals(1, response.get(0).getChildren().get(0).getChildren().size());
    }

    private CategoryDO buildCategory(Long id, String name, Long parentId, Integer level) {
        CategoryDO category = new CategoryDO();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSort(10);
        category.setStatus(1);
        return category;
    }
}
