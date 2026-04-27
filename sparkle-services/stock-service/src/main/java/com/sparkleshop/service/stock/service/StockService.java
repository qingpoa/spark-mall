package com.sparkleshop.service.stock.service;

import com.sparkleshop.service.stock.dto.internal.StockLockRequest;
import com.sparkleshop.service.stock.dto.internal.StockConfirmRequest;
import com.sparkleshop.service.stock.dto.internal.StockUnlockRequest;

public interface StockService {

    void lockStock(StockLockRequest request);

    void confirmStock(StockConfirmRequest request);

    void unlockStock(StockUnlockRequest request);
}
