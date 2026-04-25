package com.sparkleshop.service.stock.service.impl;

import com.sparkleshop.service.stock.dto.internal.StockLockRequest;
import com.sparkleshop.service.stock.service.StockService;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl implements StockService {

    @Override
    public void lockStock(StockLockRequest request) {
        // 库存服务一期先补齐订单侧调用契约，后续在这里接入真实锁库存逻辑。
    }
}
