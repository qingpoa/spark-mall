package com.sparkleshop.service.stock.service;

import com.sparkleshop.service.stock.dto.internal.StockLockRequest;

public interface StockService {

    void lockStock(StockLockRequest request);
}
