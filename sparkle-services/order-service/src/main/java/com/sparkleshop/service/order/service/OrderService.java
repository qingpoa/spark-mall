package com.sparkleshop.service.order.service;

import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.vo.OrderSubmitTokenRespVO;
import com.sparkleshop.service.order.vo.SubmitOrderRespVO;

public interface OrderService {

    OrderSubmitTokenRespVO generateSubmitToken();

    SubmitOrderRespVO submitOrder(SubmitOrderRequest request);
}
