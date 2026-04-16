package com.sparkleshop.service.product.service.impl;

import com.sparkleshop.service.product.mapper.BrandMapper;
import com.sparkleshop.service.product.service.BrandService;
import com.sparkleshop.service.product.vo.ProductBrandRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;

    @Override
    public List<ProductBrandRespVO> getEnabledBrandList(String keyword) {
        return brandMapper.selectEnabledList(keyword).stream()
                .map(brand -> {
                    ProductBrandRespVO response = new ProductBrandRespVO();
                    response.setId(brand.getId());
                    response.setName(brand.getName());
                    response.setLogo(brand.getLogo());
                    response.setDescription(brand.getDescription());
                    response.setSort(brand.getSort());
                    return response;
                })
                .toList();
    }
}
