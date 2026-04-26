package com.sparkleshop.service.order.service;

import com.sparkleshop.service.order.dto.OrderListQueryRequest;
import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.vo.OrderSubmitTokenRespVO;
import com.sparkleshop.service.order.vo.OrderListRespVO;
import com.sparkleshop.service.order.vo.SubmitOrderRespVO;

public interface OrderService {

    OrderSubmitTokenRespVO generateSubmitToken();

    OrderListRespVO getOrderList(OrderListQueryRequest request);

    SubmitOrderRespVO submitOrder(SubmitOrderRequest request);
}
