package com.sparkleshop.service.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.order.api.cart.CartFeignClient;
import com.sparkleshop.service.order.api.coupon.CouponFeignClient;
import com.sparkleshop.service.order.api.product.ProductFeignClient;
import com.sparkleshop.service.order.api.stock.StockFeignClient;
import com.sparkleshop.service.order.api.user.UserFeignClient;
import com.sparkleshop.service.order.constant.OrderErrorCodes;
import com.sparkleshop.service.order.constant.OrderRedisKeys;
import com.sparkleshop.service.order.dto.MockPayRequest;
import com.sparkleshop.service.order.dto.OrderListQueryRequest;
import com.sparkleshop.service.order.dto.SubmitOrderItemRequest;
import com.sparkleshop.service.order.dto.SubmitOrderRequest;
import com.sparkleshop.service.order.dto.internal.cart.CartClearItemsRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponOccupyRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponRollbackRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponUseRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponValidateRequest;
import com.sparkleshop.service.order.dto.internal.coupon.CouponValidateRespDTO;
import com.sparkleshop.service.order.dto.internal.product.ProductSkuSnapshotRequest;
import com.sparkleshop.service.order.dto.internal.product.ProductSkuSnapshotRespDTO;
import com.sparkleshop.service.order.dto.internal.stock.StockConfirmItemRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockConfirmRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockLockItemRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockLockRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockUnlockItemRequest;
import com.sparkleshop.service.order.dto.internal.stock.StockUnlockRequest;
import com.sparkleshop.service.order.dto.internal.user.AddressDetailRequest;
import com.sparkleshop.service.order.dto.internal.user.AddressDetailRespDTO;
import com.sparkleshop.service.order.entity.OrderDO;
import com.sparkleshop.service.order.entity.OrderItemDO;
import com.sparkleshop.service.order.entity.OrderOperateLogDO;
import com.sparkleshop.service.order.enums.OrderStatusEnum;
import com.sparkleshop.service.order.mapper.OrderItemMapper;
import com.sparkleshop.service.order.mapper.OrderMapper;
import com.sparkleshop.service.order.mapper.OrderOperateLogMapper;
import com.sparkleshop.service.order.service.OrderService;
import com.sparkleshop.service.order.vo.OrderDetailRespVO;
import com.sparkleshop.service.order.vo.OrderListRespVO;
import com.sparkleshop.service.order.vo.OrderPayTokenRespVO;
import com.sparkleshop.service.order.vo.OrderSubmitTokenRespVO;
import com.sparkleshop.service.order.vo.SubmitOrderRespVO;
import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long ORDER_EXPIRE_MINUTES = 30L;
    private static final int ORDER_LOG_STATUS_INIT = 0;
    private static final int ORDER_OPERATOR_TYPE_USER = 1;
    private static final String ORDER_ACTION_CREATE = "CREATE_ORDER";
    private static final String ORDER_ACTION_CANCEL = "CANCEL_ORDER";
    private static final String ORDER_ACTION_PAY = "PAY_ORDER";

    private final UserFeignClient userFeignClient;
    private final CartFeignClient cartFeignClient;
    private final CouponFeignClient couponFeignClient;
    private final ProductFeignClient productFeignClient;
    private final StockFeignClient stockFeignClient;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderOperateLogMapper orderOperateLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Long userId = LoginUserContext.getRequiredUserId();
        OrderDO order = getUserOrder(userId, orderId);
        validateCancelableOrder(order);
        Integer beforeStatus = order.getStatus();
        cancelUserOrder(userId, order);
        unlockStock(order);
        rollbackCouponIfNecessary(userId, order);
        saveCancelOrderOperateLog(order.getId(), userId, beforeStatus, OrderStatusEnum.CANCELED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(Long orderId, MockPayRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        consumePayToken(userId, orderId, request.getPayToken());
        OrderDO order = getUserOrder(userId, orderId);
        validatePayableOrder(order);
        Integer beforeStatus = order.getStatus();
        payUserOrder(userId, order);
        confirmStock(order);
        useCouponIfNecessary(userId, order);
        savePayOrderOperateLog(order.getId(), userId, beforeStatus, OrderStatusEnum.PAID.getCode());
    }

    @Override
    public OrderPayTokenRespVO generatePayToken(Long orderId) {
        Long userId = LoginUserContext.getRequiredUserId();
        OrderDO order = getUserOrder(userId, orderId);
        validatePayableOrder(order);

        String payToken = UUID.fastUUID().toString(true);
        stringRedisTemplate.opsForValue().set(
                OrderRedisKeys.payToken(userId, orderId),
                payToken,
                OrderRedisKeys.ORDER_PAY_TOKEN_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        OrderPayTokenRespVO response = new OrderPayTokenRespVO();
        response.setPayToken(payToken);
        return response;
    }

    @Override
    public OrderSubmitTokenRespVO generateSubmitToken() {
        Long userId = LoginUserContext.getRequiredUserId();
        String submitToken = UUID.fastUUID().toString(true);
        String redisKey = OrderRedisKeys.submitToken(userId);
        stringRedisTemplate.opsForValue().set(
                redisKey,
                submitToken,
                OrderRedisKeys.ORDER_SUBMIT_TOKEN_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        OrderSubmitTokenRespVO response = new OrderSubmitTokenRespVO();
        response.setSubmitToken(submitToken);
        return response;
    }

    @Override
    public OrderDetailRespVO getOrderDetail(Long orderId) {
        Long userId = LoginUserContext.getRequiredUserId();
        OrderDO order = getUserOrder(userId, orderId);

        List<OrderItemDO> orderItems = orderItemMapper.selectByOrderId(orderId);

        OrderDetailRespVO response = new OrderDetailRespVO();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setActualAmount(order.getActualAmount());
        response.setCouponId(order.getCouponId());
        response.setReceiverName(order.getReceiverName());
        response.setReceiverMobile(order.getReceiverMobile());
        response.setReceiverAddress(order.getReceiverAddress());
        response.setPayTime(order.getPayTime());
        response.setCancelTime(order.getCancelTime());
        response.setCloseTime(order.getCloseTime());
        response.setCompleteTime(order.getCompleteTime());
        response.setExpireTime(order.getExpireTime());
        response.setCreateTime(order.getCreateTime());

        List<OrderDetailRespVO.OrderItem> items = new ArrayList<>(orderItems.size());
        for (OrderItemDO orderItem : orderItems) {
            OrderDetailRespVO.OrderItem item = new OrderDetailRespVO.OrderItem();
            item.setSkuId(orderItem.getSkuId());
            item.setSpuId(orderItem.getSpuId());
            item.setSkuName(orderItem.getSkuName());
            item.setSpuName(orderItem.getSpuName());
            item.setQuantity(orderItem.getQuantity());
            item.setPrice(orderItem.getPrice());
            item.setTotalPrice(orderItem.getTotalPrice());
            item.setSpecJson(orderItem.getSpecJson());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    @Override
    public OrderListRespVO getOrderList(OrderListQueryRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        Integer status = request.getStatus();
        Page<OrderDO> page = new Page<>(request.getPageNo(), request.getPageSize());
        Page<OrderDO> orderPage = orderMapper.selectUserOrderPage(page, userId, status);

        OrderListRespVO response = new OrderListRespVO();
        response.setTotal(orderPage.getTotal());
        response.setPageNo(orderPage.getCurrent());
        response.setPageSize(orderPage.getSize());

        List<OrderDO> orders = orderPage.getRecords();
        if (orders == null || orders.isEmpty()) {
            response.setList(Collections.emptyList());
            return response;
        }

        List<Long> orderIds = orders.stream()
                .map(OrderDO::getId)
                .toList();
        List<OrderItemDO> orderItems = orderItemMapper.selectByOrderIds(orderIds);
        Map<Long, List<OrderItemDO>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemDO::getOrderId, LinkedHashMap::new, Collectors.toList()));

        List<OrderListRespVO.Item> list = new ArrayList<>(orders.size());
        for (OrderDO order : orders) {
            OrderListRespVO.Item item = new OrderListRespVO.Item();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setStatus(order.getStatus());
            item.setActualAmount(order.getActualAmount());
            item.setCreateTime(order.getCreateTime());

            List<OrderItemDO> currentOrderItems = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
            item.setItemCount(currentOrderItems.stream()
                    .map(OrderItemDO::getQuantity)
                    .filter(quantity -> quantity != null && quantity > 0)
                    .reduce(0, Integer::sum));

            List<OrderListRespVO.OrderItem> summaries = new ArrayList<>(currentOrderItems.size());
            for (OrderItemDO orderItem : currentOrderItems) {
                OrderListRespVO.OrderItem summary = new OrderListRespVO.OrderItem();
                summary.setSkuId(orderItem.getSkuId());
                summary.setSkuName(orderItem.getSkuName());
                summary.setQuantity(orderItem.getQuantity());
                summary.setPrice(orderItem.getPrice());
                summary.setTotalPrice(orderItem.getTotalPrice());
                summaries.add(summary);
            }

            item.setItems(summaries);
            list.add(item);
        }

        response.setList(list);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitOrderRespVO submitOrder(SubmitOrderRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();

        ValidateSubmitOrderData validateData = validateSubmitOrderRequest(userId, request);
        OrderAmountData orderAmountData = calculateOrderAmount(validateData, request.getCouponId());
        String orderNo = generateOrderNo();
        lockStock(orderNo, validateData.items());
        OrderDO order = saveOrder(orderNo, request, validateData, orderAmountData);
        saveOrderItems(order.getId(), validateData);
        saveOrderOperateLog(order.getId(), userId, OrderStatusEnum.PENDING_PAYMENT.getCode());
        occupyCouponIfNecessary(userId, request.getCouponId(), order.getId());
        clearCartItemsAfterCommit(userId, validateData.items());

        SubmitOrderRespVO response = new SubmitOrderRespVO();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setExpireTime(order.getExpireTime());
        return response;
    }

    private ValidateSubmitOrderData validateSubmitOrderRequest(Long userId, SubmitOrderRequest request) {
        List<Long> skuIds = validateAndExtractSkuIds(request.getItems());
        consumeSubmitToken(userId, request.getSubmitToken());

        AddressDetailRespDTO address = getAddressDetail(userId, request.getAddressId());
        List<ProductSkuSnapshotRespDTO> productSnapshots = getProductSnapshots(skuIds);
        if (productSnapshots.size() != skuIds.size()) {
            throw new BusinessException(OrderErrorCodes.RESOURCE_NOT_FOUND, "存在不存在的商品");
        }

        return new ValidateSubmitOrderData(userId, address, productSnapshots, request.getItems());
    }

    private OrderDO getUserOrder(Long userId, Long orderId) {
        OrderDO order = orderMapper.selectUserOrderById(userId, orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void validateCancelableOrder(OrderDO order) {
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
            throw new BusinessException(OrderErrorCodes.ORDER_STATUS_INVALID, "当前订单状态不允许取消");
        }
    }

    private void validatePayableOrder(OrderDO order) {
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getStatus())) {
            throw new BusinessException(OrderErrorCodes.ORDER_STATUS_INVALID, "当前订单状态不允许支付");
        }
    }

    private void cancelUserOrder(Long userId, OrderDO order) {
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.updateUserOrderStatusIfMatch(
                userId,
                order.getId(),
                OrderStatusEnum.PENDING_PAYMENT.getCode(),
                OrderStatusEnum.CANCELED.getCode(),
                now,
                now
        );
        if (rows != 1) {
            throw new BusinessException(OrderErrorCodes.ORDER_STATUS_INVALID, "订单状态已变更，取消失败");
        }
        order.setStatus(OrderStatusEnum.CANCELED.getCode());
        order.setCancelTime(now);
        order.setUpdateTime(now);
    }

    private void payUserOrder(Long userId, OrderDO order) {
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.updateUserOrderPaidIfMatch(
                userId,
                order.getId(),
                OrderStatusEnum.PENDING_PAYMENT.getCode(),
                OrderStatusEnum.PAID.getCode(),
                now,
                now
        );
        if (rows != 1) {
            throw new BusinessException(OrderErrorCodes.ORDER_STATUS_INVALID, "订单状态已变更，支付失败");
        }
        order.setStatus(OrderStatusEnum.PAID.getCode());
        order.setPayTime(now);
        order.setUpdateTime(now);
    }

    private List<Long> validateAndExtractSkuIds(List<SubmitOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "下单商品不能为空");
        }

        List<Long> skuIds = new ArrayList<>(items.size());
        Set<Long> seenSkuIds = new HashSet<>();
        for (SubmitOrderItemRequest item : items) {
            if (item == null || item.getSkuId() == null) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "下单商品不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "商品数量必须大于 0");
            }
            if (!seenSkuIds.add(item.getSkuId())) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "订单商品不能重复");
            }
            skuIds.add(item.getSkuId());
        }
        return skuIds;
    }

    private void consumeSubmitToken(Long userId, String submitToken) {
        String redisKey = OrderRedisKeys.submitToken(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local current = redis.call('get', KEYS[1])
                if current == ARGV[1] then
                    redis.call('del', KEYS[1])
                    return 1
                end
                return 0
                """);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(script, Collections.singletonList(redisKey), submitToken);
        if (!Long.valueOf(1L).equals(result)) {
            throw new BusinessException(OrderErrorCodes.REPEAT_SUBMIT, "请勿重复提交订单");
        }
    }

    private void consumePayToken(Long userId, Long orderId, String payToken) {
        String redisKey = OrderRedisKeys.payToken(userId, orderId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local current = redis.call('get', KEYS[1])
                if current == ARGV[1] then
                    redis.call('del', KEYS[1])
                    return 1
                end
                return 0
                """);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(script, Collections.singletonList(redisKey), payToken);
        if (!Long.valueOf(1L).equals(result)) {
            throw new BusinessException(OrderErrorCodes.REPEAT_SUBMIT, "请勿重复支付提交");
        }
    }

    private AddressDetailRespDTO getAddressDetail(Long userId, Long addressId) {
        AddressDetailRequest request = new AddressDetailRequest();
        request.setUserId(userId);
        request.setAddressId(addressId);

        Result result = userFeignClient.getAddressDetail(request);
        if (result == null || result.getCode() == null || result.getCode() != 0 || result.getData() == null) {
            throw new BusinessException(OrderErrorCodes.ADDRESS_NOT_FOUND, "收货地址不存在");
        }
        return objectMapper.convertValue(result.getData(), AddressDetailRespDTO.class);
    }

    private List<ProductSkuSnapshotRespDTO> getProductSnapshots(List<Long> skuIds) {
        ProductSkuSnapshotRequest request = new ProductSkuSnapshotRequest();
        request.setSkuIds(skuIds);

        Result result = productFeignClient.getSkuSnapshots(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(Result.SERVER_ERROR, "调用商品服务查询 SKU 快照失败");
        }
        return objectMapper.convertValue(result.getData(), new TypeReference<List<ProductSkuSnapshotRespDTO>>() {
        });
    }

    private OrderAmountData calculateOrderAmount(ValidateSubmitOrderData validateData, Long couponId) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Integer> quantityMap = new HashMap<>();
        for (SubmitOrderItemRequest item : validateData.items()) {
            quantityMap.put(item.getSkuId(), item.getQuantity());
        }

        for (ProductSkuSnapshotRespDTO snapshot : validateData.productSnapshots()) {
            if (!Boolean.TRUE.equals(snapshot.getAvailable())) {
                throw new BusinessException(OrderErrorCodes.RESOURCE_NOT_FOUND, "商品已下架或不可售");
            }

            Integer quantity = quantityMap.get(snapshot.getSkuId());
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "下单商品数量异常");
            }

            int stock = snapshot.getStock() == null ? 0 : snapshot.getStock();
            if (stock < quantity) {
                throw new BusinessException(OrderErrorCodes.PRODUCT_STOCK_INSUFFICIENT, "商品库存不足");
            }

            BigDecimal price = snapshot.getPrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "商品价格异常");
            }

            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
        }

        BigDecimal discountAmount = validateCoupon(validateData.userId(), couponId, totalAmount);
        BigDecimal actualAmount = totalAmount.subtract(discountAmount);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "优惠金额不能大于订单总金额");
        }

        return new OrderAmountData(totalAmount, discountAmount, actualAmount);
    }

    private BigDecimal validateCoupon(Long userId, Long couponId, BigDecimal totalAmount) {
        if (couponId == null) {
            return BigDecimal.ZERO;
        }

        CouponValidateRequest request = new CouponValidateRequest();
        request.setUserId(userId);
        request.setCouponId(couponId);
        request.setOrderAmount(totalAmount);

        Result result = couponFeignClient.validateCoupon(request);
        if (result == null || result.getCode() == null || result.getCode() != 0 || result.getData() == null) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "优惠券不可用");
        }

        CouponValidateRespDTO response = objectMapper.convertValue(result.getData(), CouponValidateRespDTO.class);
        if (!Boolean.TRUE.equals(response.getAvailable())) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "优惠券不可用");
        }

        BigDecimal discountAmount = response.getDiscountAmount();
        if (discountAmount == null) {
            return BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "优惠金额异常");
        }
        return discountAmount;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        int randomSuffix = RandomUtil.randomInt(1000, 10000);
        return timestamp + randomSuffix;
    }

    private void lockStock(String orderNo, List<SubmitOrderItemRequest> items) {
        StockLockRequest request = new StockLockRequest();
        request.setOrderNo(orderNo);

        List<StockLockItemRequest> lockItems = new ArrayList<>(items.size());
        for (SubmitOrderItemRequest item : items) {
            StockLockItemRequest lockItem = new StockLockItemRequest();
            lockItem.setSkuId(item.getSkuId());
            lockItem.setQuantity(item.getQuantity());
            lockItems.add(lockItem);
        }
        request.setItems(lockItems);

        Result result = stockFeignClient.lockStock(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(OrderErrorCodes.PRODUCT_STOCK_INSUFFICIENT, "锁定库存失败");
        }
    }

    private void unlockStock(OrderDO order) {
        List<OrderItemDO> orderItems = orderItemMapper.selectByOrderId(order.getId());
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        StockUnlockRequest request = new StockUnlockRequest();
        request.setOrderNo(order.getOrderNo());

        List<StockUnlockItemRequest> unlockItems = new ArrayList<>(orderItems.size());
        for (OrderItemDO orderItem : orderItems) {
            StockUnlockItemRequest unlockItem = new StockUnlockItemRequest();
            unlockItem.setSkuId(orderItem.getSkuId());
            unlockItem.setQuantity(orderItem.getQuantity());
            unlockItems.add(unlockItem);
        }
        request.setItems(unlockItems);

        Result result = stockFeignClient.unlockStock(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(Result.SERVER_ERROR, "释放库存失败");
        }
    }

    private void confirmStock(OrderDO order) {
        List<OrderItemDO> orderItems = orderItemMapper.selectByOrderId(order.getId());
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        StockConfirmRequest request = new StockConfirmRequest();
        request.setOrderNo(order.getOrderNo());

        List<StockConfirmItemRequest> confirmItems = new ArrayList<>(orderItems.size());
        for (OrderItemDO orderItem : orderItems) {
            StockConfirmItemRequest confirmItem = new StockConfirmItemRequest();
            confirmItem.setSkuId(orderItem.getSkuId());
            confirmItem.setQuantity(orderItem.getQuantity());
            confirmItems.add(confirmItem);
        }
        request.setItems(confirmItems);

        Result result = stockFeignClient.confirmStock(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(Result.SERVER_ERROR, "确认库存扣减失败");
        }
    }

    private OrderDO saveOrder(String orderNo,
                              SubmitOrderRequest request,
                              ValidateSubmitOrderData validateData,
                              OrderAmountData orderAmountData) {
        LocalDateTime now = LocalDateTime.now();

        OrderDO order = new OrderDO();
        order.setOrderNo(orderNo);
        order.setUserId(validateData.userId());
        order.setAddressId(request.getAddressId());
        order.setReceiverName(validateData.address().getReceiverName());
        order.setReceiverMobile(validateData.address().getReceiverMobile());
        order.setReceiverAddress(buildReceiverAddress(validateData.address()));
        order.setTotalAmount(orderAmountData.totalAmount());
        order.setDiscountAmount(orderAmountData.discountAmount());
        order.setActualAmount(orderAmountData.actualAmount());
        order.setCouponId(request.getCouponId());
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setExpireTime(now.plusMinutes(ORDER_EXPIRE_MINUTES));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);

        int rows = orderMapper.insert(order);
        if (rows != 1 || order.getId() == null) {
            throw new BusinessException(Result.SERVER_ERROR, "保存订单主表失败");
        }
        return order;
    }

    private void saveOrderItems(Long orderId, ValidateSubmitOrderData validateData) {
        Map<Long, Integer> quantityMap = new HashMap<>();
        for (SubmitOrderItemRequest item : validateData.items()) {
            quantityMap.put(item.getSkuId(), item.getQuantity());
        }

        LocalDateTime now = LocalDateTime.now();
        for (ProductSkuSnapshotRespDTO snapshot : validateData.productSnapshots()) {
            Integer quantity = quantityMap.get(snapshot.getSkuId());
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(OrderErrorCodes.INVALID_REQUEST, "下单商品数量异常");
            }

            OrderItemDO orderItem = new OrderItemDO();
            orderItem.setOrderId(orderId);
            orderItem.setSkuId(snapshot.getSkuId());
            orderItem.setSpuId(snapshot.getSpuId());
            orderItem.setSkuName(snapshot.getSkuName());
            orderItem.setSpuName(snapshot.getSpuName());
            orderItem.setQuantity(quantity);
            orderItem.setPrice(snapshot.getPrice());
            orderItem.setTotalPrice(snapshot.getPrice().multiply(BigDecimal.valueOf(quantity)));
            orderItem.setSpecJson(writeSpecJson(snapshot));
            orderItem.setCreateTime(now);
            orderItem.setUpdateTime(now);
            orderItem.setDeleted(0);

            int rows = orderItemMapper.insert(orderItem);
            if (rows != 1 || orderItem.getId() == null) {
                throw new BusinessException(Result.SERVER_ERROR, "保存订单明细失败");
            }
        }
    }

    private String writeSpecJson(ProductSkuSnapshotRespDTO snapshot) {
        if (snapshot.getSpec() == null || snapshot.getSpec().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot.getSpec());
        } catch (Exception e) {
            throw new BusinessException(Result.SERVER_ERROR, "订单规格快照序列化失败");
        }
    }

    private void saveOrderOperateLog(Long orderId, Long userId, Integer afterStatus) {
        LocalDateTime now = LocalDateTime.now();

        OrderOperateLogDO log = new OrderOperateLogDO();
        log.setOrderId(orderId);
        log.setOperatorType(ORDER_OPERATOR_TYPE_USER);
        log.setOperatorId(userId);
        log.setAction(ORDER_ACTION_CREATE);
        log.setBeforeStatus(ORDER_LOG_STATUS_INIT);
        log.setAfterStatus(afterStatus);
        log.setRemark("用户提交订单");
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setDeleted(0);

        int rows = orderOperateLogMapper.insert(log);
        if (rows != 1 || log.getId() == null) {
            throw new BusinessException(Result.SERVER_ERROR, "保存订单操作日志失败");
        }
    }

    private void saveCancelOrderOperateLog(Long orderId, Long userId, Integer beforeStatus, Integer afterStatus) {
        LocalDateTime now = LocalDateTime.now();

        OrderOperateLogDO log = new OrderOperateLogDO();
        log.setOrderId(orderId);
        log.setOperatorType(ORDER_OPERATOR_TYPE_USER);
        log.setOperatorId(userId);
        log.setAction(ORDER_ACTION_CANCEL);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setRemark("用户取消订单");
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setDeleted(0);

        int rows = orderOperateLogMapper.insert(log);
        if (rows != 1 || log.getId() == null) {
            throw new BusinessException(Result.SERVER_ERROR, "保存取消订单操作日志失败");
        }
    }

    private void savePayOrderOperateLog(Long orderId, Long userId, Integer beforeStatus, Integer afterStatus) {
        LocalDateTime now = LocalDateTime.now();

        OrderOperateLogDO log = new OrderOperateLogDO();
        log.setOrderId(orderId);
        log.setOperatorType(ORDER_OPERATOR_TYPE_USER);
        log.setOperatorId(userId);
        log.setAction(ORDER_ACTION_PAY);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setRemark("用户模拟支付成功");
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setDeleted(0);

        int rows = orderOperateLogMapper.insert(log);
        if (rows != 1 || log.getId() == null) {
            throw new BusinessException(Result.SERVER_ERROR, "保存支付订单操作日志失败");
        }
    }

    private void occupyCouponIfNecessary(Long userId, Long couponId, Long orderId) {
        if (couponId == null) {
            return;
        }

        CouponOccupyRequest request = new CouponOccupyRequest();
        request.setUserId(userId);
        request.setCouponId(couponId);
        request.setOrderId(orderId);

        Result result = couponFeignClient.occupyCoupon(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "占用优惠券失败");
        }
    }

    private void useCouponIfNecessary(Long userId, OrderDO order) {
        if (order.getCouponId() == null) {
            return;
        }

        CouponUseRequest request = new CouponUseRequest();
        request.setUserId(userId);
        request.setCouponId(order.getCouponId());
        request.setOrderId(order.getId());

        Result result = couponFeignClient.useCoupon(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "核销优惠券失败");
        }
    }

    private void rollbackCouponIfNecessary(Long userId, OrderDO order) {
        if (order.getCouponId() == null) {
            return;
        }

        CouponRollbackRequest request = new CouponRollbackRequest();
        request.setUserId(userId);
        request.setCouponId(order.getCouponId());
        request.setOrderId(order.getId());

        Result result = couponFeignClient.rollbackCoupon(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(OrderErrorCodes.COUPON_UNAVAILABLE, "回退优惠券失败");
        }
    }

    private void clearCartItems(Long userId, List<SubmitOrderItemRequest> items) {
        CartClearItemsRequest request = new CartClearItemsRequest();
        request.setUserId(userId);

        List<Long> skuIds = new ArrayList<>(items.size());
        for (SubmitOrderItemRequest item : items) {
            skuIds.add(item.getSkuId());
        }
        request.setSkuIds(skuIds);

        Result result = cartFeignClient.clearCheckedItems(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(Result.SERVER_ERROR, "清理购物车失败");
        }
    }

    private void clearCartItemsAfterCommit(Long userId, List<SubmitOrderItemRequest> items) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            clearCartItems(userId, items);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    clearCartItems(userId, items);
                } catch (Exception e) {
                    log.warn("clear cart after order submit failed, userId={}, skuCount={}",
                            userId,
                            items == null ? 0 : items.size(),
                            e);
                }
            }
        });
    }

    private String buildReceiverAddress(AddressDetailRespDTO address) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, address.getProvince());
        appendAddressPart(builder, address.getCity());
        appendAddressPart(builder, address.getDistrict());
        appendAddressPart(builder, address.getDetailAddress());
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(value);
    }

    private record ValidateSubmitOrderData(
            Long userId,
            AddressDetailRespDTO address,
            List<ProductSkuSnapshotRespDTO> productSnapshots,
            List<SubmitOrderItemRequest> items
    ) {
    }

    private record OrderAmountData(
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal actualAmount
    ) {
    }
}
