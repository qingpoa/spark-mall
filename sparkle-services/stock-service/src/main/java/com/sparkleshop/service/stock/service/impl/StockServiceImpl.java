package com.sparkleshop.service.stock.service.impl;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.service.stock.dto.internal.StockConfirmItemRequest;
import com.sparkleshop.service.stock.dto.internal.StockConfirmRequest;
import com.sparkleshop.service.stock.dto.internal.StockLockRequest;
import com.sparkleshop.service.stock.dto.internal.StockLockItemRequest;
import com.sparkleshop.service.stock.dto.internal.StockUnlockRequest;
import com.sparkleshop.service.stock.dto.internal.StockUnlockItemRequest;
import com.sparkleshop.service.stock.constant.StockErrorCodes;
import com.sparkleshop.service.stock.entity.SkuStockDO;
import com.sparkleshop.service.stock.mapper.SkuStockMapper;
import com.sparkleshop.service.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final SkuStockMapper skuStockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(StockLockRequest request) {
        List<StockLockItemRequest> lockItems = mergeLockItems(request);
        for (StockLockItemRequest item : lockItems) {
            int rows = skuStockMapper.lockStock(item.getSkuId(), item.getQuantity());
            if (rows == 1) {
                continue;
            }

            SkuStockDO skuStock = skuStockMapper.selectBySkuId(item.getSkuId());
            if (skuStock == null) {
                throw new BusinessException(StockErrorCodes.RESOURCE_NOT_FOUND, "库存记录不存在");
            }
            throw new BusinessException(StockErrorCodes.PRODUCT_STOCK_INSUFFICIENT, "商品库存不足");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockStock(StockUnlockRequest request) {
        List<StockUnlockItemRequest> unlockItems = mergeUnlockItems(request);
        for (StockUnlockItemRequest item : unlockItems) {
            int rows = skuStockMapper.unlockStock(item.getSkuId(), item.getQuantity());
            if (rows == 1) {
                continue;
            }

            SkuStockDO skuStock = skuStockMapper.selectBySkuId(item.getSkuId());
            if (skuStock == null) {
                throw new BusinessException(StockErrorCodes.RESOURCE_NOT_FOUND, "库存记录不存在");
            }
            throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "锁定库存不足，无法释放");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmStock(StockConfirmRequest request) {
        List<StockConfirmItemRequest> confirmItems = mergeConfirmItems(request);
        for (StockConfirmItemRequest item : confirmItems) {
            int rows = skuStockMapper.confirmStock(item.getSkuId(), item.getQuantity());
            if (rows == 1) {
                continue;
            }

            SkuStockDO skuStock = skuStockMapper.selectBySkuId(item.getSkuId());
            if (skuStock == null) {
                throw new BusinessException(StockErrorCodes.RESOURCE_NOT_FOUND, "库存记录不存在");
            }
            throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "锁定库存不足，无法确认扣减");
        }
    }

    private List<StockLockItemRequest> mergeLockItems(StockLockRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "锁库存商品不能为空");
        }

        Map<Long, Integer> quantityMap = new HashMap<>();
        for (StockLockItemRequest item : request.getItems()) {
            if (item == null || item.getSkuId() == null) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "skuId 不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "锁库存数量必须大于 0");
            }
            quantityMap.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        List<StockLockItemRequest> items = new ArrayList<>(quantityMap.size());
        quantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    StockLockItemRequest item = new StockLockItemRequest();
                    item.setSkuId(entry.getKey());
                    item.setQuantity(entry.getValue());
                    items.add(item);
                });
        return items;
    }

    private List<StockUnlockItemRequest> mergeUnlockItems(StockUnlockRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "解锁库存商品不能为空");
        }

        Map<Long, Integer> quantityMap = new HashMap<>();
        for (StockUnlockItemRequest item : request.getItems()) {
            if (item == null || item.getSkuId() == null) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "skuId 不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "解锁库存数量必须大于 0");
            }
            quantityMap.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        List<StockUnlockItemRequest> items = new ArrayList<>(quantityMap.size());
        quantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    StockUnlockItemRequest item = new StockUnlockItemRequest();
                    item.setSkuId(entry.getKey());
                    item.setQuantity(entry.getValue());
                    items.add(item);
                });
        return items;
    }

    private List<StockConfirmItemRequest> mergeConfirmItems(StockConfirmRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "确认扣减商品不能为空");
        }

        Map<Long, Integer> quantityMap = new HashMap<>();
        for (StockConfirmItemRequest item : request.getItems()) {
            if (item == null || item.getSkuId() == null) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "skuId 不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(StockErrorCodes.INVALID_REQUEST, "确认扣减数量必须大于 0");
            }
            quantityMap.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        List<StockConfirmItemRequest> items = new ArrayList<>(quantityMap.size());
        quantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    StockConfirmItemRequest item = new StockConfirmItemRequest();
                    item.setSkuId(entry.getKey());
                    item.setQuantity(entry.getValue());
                    items.add(item);
                });
        return items;
    }
}
