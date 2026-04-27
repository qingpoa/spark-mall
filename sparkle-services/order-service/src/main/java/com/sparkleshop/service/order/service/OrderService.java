package com.sparkleshop.service.order.service;

import com.sparkleshop.service.order.dto.OrderListQueryRequest;
import com.sparkleshop.service.order.dto.MockPayRequest;
import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.vo.OrderDetailRespVO;
import com.sparkleshop.service.order.vo.OrderSubmitTokenRespVO;
import com.sparkleshop.service.order.vo.OrderListRespVO;
import com.sparkleshop.service.order.vo.OrderPayTokenRespVO;
import com.sparkleshop.service.order.vo.SubmitOrderRespVO;

public interface OrderService {

    void cancelOrder(Long orderId);

    void mockPay(Long orderId, MockPayRequest request);

    OrderPayTokenRespVO generatePayToken(Long orderId);

    OrderSubmitTokenRespVO generateSubmitToken();

    OrderDetailRespVO getOrderDetail(Long orderId);

    OrderListRespVO getOrderList(OrderListQueryRequest request);

    SubmitOrderRespVO submitOrder(SubmitOrderRequest request);
}
