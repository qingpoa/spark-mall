package com.sparkleshop.service.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.cart.api.product.ProductFeignClient;
import com.sparkleshop.service.cart.constant.CartRedisKeys;
import com.sparkleshop.service.cart.dto.CartAddRequest;
import com.sparkleshop.service.cart.dto.CartSelectAllRequest;
import com.sparkleshop.service.cart.dto.CartSelectRequest;
import com.sparkleshop.service.cart.dto.CartUpdateRequest;
import com.sparkleshop.service.cart.dto.internal.ProductSkuSnapshotRequest;
import com.sparkleshop.service.cart.dto.internal.ProductSkuSnapshotRespDTO;
import com.sparkleshop.service.cart.entity.CartItemCacheDO;
import com.sparkleshop.service.cart.service.CartService;
import com.sparkleshop.service.cart.vo.CartAddRespVO;
import com.sparkleshop.service.cart.vo.CartItemRespVO;
import com.sparkleshop.service.cart.vo.CartListRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ProductFeignClient productFeignClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CartListRespVO getCartList() {
        Long userId = LoginUserContext.getRequiredUserId();

        CartListRespVO response = new CartListRespVO();
        String redisKey = CartRedisKeys.cart(userId);
        Map<Object, Object> cart = stringRedisTemplate.opsForHash().entries(redisKey);
        if (cart.isEmpty()) {
            response.setItems(Collections.emptyList());
            response.setTotalAmount(BigDecimal.ZERO);
            response.setSelectedAmount(BigDecimal.ZERO);
            response.setSelectedCount(0);
            return response;
        }

        Map<Long, CartItemCacheDO> cartItemMap = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : cart.entrySet()) {
            Long skuId = Long.parseLong(entry.getKey().toString());
            CartItemCacheDO cartItemCacheDO;
            try {
                cartItemCacheDO = objectMapper.readValue(String.valueOf(entry.getValue()), CartItemCacheDO.class);
            } catch (JsonProcessingException e) {
                throw new BusinessException(Result.SERVER_ERROR, "购物车数据格式错误");
            }
            cartItemMap.put(skuId, cartItemCacheDO);
        }


        List<Long> skuIds = new ArrayList<>(cartItemMap.keySet());
        List<ProductSkuSnapshotRespDTO> snapshots = getProductSnapshots(skuIds);

        Map<Long, ProductSkuSnapshotRespDTO> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(ProductSkuSnapshotRespDTO::getSkuId, Function.identity(), (left, right) -> left));

        List<CartItemRespVO> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        int selectedCount = 0;

        for (Map.Entry<Long, CartItemCacheDO> entry : cartItemMap.entrySet()) {
            Long skuId = entry.getKey();
            CartItemCacheDO cacheDO = entry.getValue();
            ProductSkuSnapshotRespDTO snapshot = snapshotMap.get(skuId);
            Integer quantity = cacheDO.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(Result.SERVER_ERROR, "购物车数据异常");
            }

            CartItemRespVO item = new CartItemRespVO();
            item.setSkuId(skuId);
            item.setQuantity(quantity);
            item.setSelected(Boolean.TRUE.equals(cacheDO.getSelected()));

            if (snapshot != null) {
                item.setSpuId(snapshot.getSpuId());
                item.setName(snapshot.getSkuName());
                item.setImage(snapshot.getImage());
                item.setSpec(snapshot.getSpec());
                item.setPrice(snapshot.getPrice() == null ? BigDecimal.ZERO : snapshot.getPrice());
                item.setStock(snapshot.getStock() == null ? 0 : snapshot.getStock());
                item.setAvailable(Boolean.TRUE.equals(snapshot.getAvailable()));
            } else {
                item.setName("商品已失效");
                item.setImage(null);
                item.setSpec(Collections.emptyMap());
                item.setPrice(BigDecimal.ZERO);
                item.setStock(0);
                item.setAvailable(false);
            }

            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(quantity));
            item.setSubtotal(subtotal);

            totalAmount = totalAmount.add(subtotal);
            if (Boolean.TRUE.equals(item.getSelected()) && Boolean.TRUE.equals(item.getAvailable())) {
                selectedAmount = selectedAmount.add(subtotal);
                selectedCount += quantity;
            }

            items.add(item);
        }

        response.setItems(items);
        response.setTotalAmount(totalAmount);
        response.setSelectedAmount(selectedAmount);
        response.setSelectedCount(selectedCount);
        return response;
    }

    @Override
    public CartAddRespVO addCart(CartAddRequest cartAddReqVO) {
        Long userId = LoginUserContext.getRequiredUserId();
        Long skuId = cartAddReqVO.getSkuId();

        List<Long> skuIds = Collections.singletonList(skuId);
        List<ProductSkuSnapshotRespDTO> snapshots = getProductSnapshots(skuIds);
        if (snapshots.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "商品不存在");
        }
        ProductSkuSnapshotRespDTO snapshot = snapshots.get(0);

        if (snapshot == null || !Boolean.TRUE.equals(snapshot.getAvailable())) {
            throw new BusinessException(Result.SERVER_ERROR, "商品已失效");
        }
        if (snapshot.getStock() < cartAddReqVO.getQuantity()) {
            throw new BusinessException(Result.SERVER_ERROR, "商品库存不足");
        }
        String redisKey = CartRedisKeys.cart(userId);
        String skuField = String.valueOf(skuId);

        Object cacheValue = stringRedisTemplate.opsForHash().get(redisKey, skuField);

        CartItemCacheDO cartItem;
        if (cacheValue == null) {
            cartItem = new CartItemCacheDO();
            cartItem.setQuantity(0);
            cartItem.setSelected(true);
            cartItem.setAddTime(LocalDateTime.now());
        } else {
            try {
                cartItem = objectMapper.readValue(String.valueOf(cacheValue), CartItemCacheDO.class);
            } catch (JsonProcessingException e) {
                throw new BusinessException(Result.SERVER_ERROR, "购物车数据格式错误");
            }
        }

        Integer oldQuantity = cartItem.getQuantity();
        if (oldQuantity == null || oldQuantity < 0) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据异常");
        }

        int newQuantity = oldQuantity + cartAddReqVO.getQuantity();
        if (newQuantity < 0) {
            throw new BusinessException(Result.SERVER_ERROR, "商品数量不能小于 0");
        }
        if(newQuantity > snapshot.getStock()){
            throw new BusinessException(Result.SERVER_ERROR, "商品库存不足");
        }
        cartItem.setQuantity(newQuantity);
        cartItem.setSelected(true);
        String json;
        try {
            json = objectMapper.writeValueAsString(cartItem);
        } catch (JsonProcessingException e) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据序列化失败");
        }
        stringRedisTemplate.opsForHash().put(redisKey, skuField, json);
        CartAddRespVO response = new CartAddRespVO();
        response.setSkuId(skuId);
        response.setQuantity(cartItem.getQuantity());
        response.setSelected(cartItem.getSelected());

        return response;
    }

    @Override
    public void updateCart(CartUpdateRequest cartUpdateReqVO) {
        Long userId = LoginUserContext.getRequiredUserId();
        Long skuId = cartUpdateReqVO.getSkuId();
        String redisKey = CartRedisKeys.cart(userId);
        String skuField = String.valueOf(skuId);
        Object cacheValue = stringRedisTemplate.opsForHash().get(redisKey, skuField);
        CartItemCacheDO cartItem;

        if (cacheValue == null) {
            throw new BusinessException(Result.NOT_FOUND, "购物车中不存在该商品");
        }
        try {
            cartItem = objectMapper.readValue(String.valueOf(cacheValue), CartItemCacheDO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据格式错误");
        }
        if (cartItem == null) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据异常");
        }

        List<Long> skuIds = Collections.singletonList(skuId);
        List<ProductSkuSnapshotRespDTO> snapshots = getProductSnapshots(skuIds);
        if (snapshots.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "商品不存在");
        }
        ProductSkuSnapshotRespDTO snapshot = snapshots.get(0);

        if (snapshot == null || !Boolean.TRUE.equals(snapshot.getAvailable())) {
            throw new BusinessException(Result.SERVER_ERROR, "商品已失效");
        }

        if (snapshot.getStock() < cartUpdateReqVO.getQuantity()) {
            throw new BusinessException(Result.SERVER_ERROR, "商品库存不足");
        }

        cartItem.setSelected(cartItem.getSelected());
        cartItem.setQuantity(cartUpdateReqVO.getQuantity());
        String json;
        try {
            json = objectMapper.writeValueAsString(cartItem);
        }catch (JsonProcessingException e){
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据序列化失败");
        }
        stringRedisTemplate.opsForHash().put(redisKey, skuField, json);

    }

    @Override
    public void deleteCartItem(Long skuId) {
        Long userId = LoginUserContext.getRequiredUserId();
        String redisKey = CartRedisKeys.cart(userId);
        String skuField = String.valueOf(skuId);
        stringRedisTemplate.opsForHash().delete(redisKey, skuField);
    }

    @Override
    public void selectCartItem(CartSelectRequest cartSelectReqVO) {
        Long userId = LoginUserContext.getRequiredUserId();
        String redisKey = CartRedisKeys.cart(userId);
        String skuField = String.valueOf(cartSelectReqVO.getSkuId());
        Object cacheValue = stringRedisTemplate.opsForHash().get(redisKey, skuField);
        CartItemCacheDO cartItem;
        if (cacheValue == null) {
            throw new BusinessException(Result.NOT_FOUND, "购物车中不存在该商品");
        }
        try {
            cartItem = objectMapper.readValue(String.valueOf(cacheValue), CartItemCacheDO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据格式错误");
        }
        if (cartItem == null) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据异常");
        }
        cartItem.setSelected(cartSelectReqVO.getSelected());
        String json;
        try {
            json = objectMapper.writeValueAsString(cartItem);
        } catch (JsonProcessingException e) {
            throw new BusinessException(Result.SERVER_ERROR, "购物车数据序列化失败");
        }
        stringRedisTemplate.opsForHash().put(redisKey, skuField, json);
    }

    @Override
    public void selectAllCartItem(CartSelectAllRequest select) {
        Long userId = LoginUserContext.getRequiredUserId();
        String redisKey = CartRedisKeys.cart(userId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(redisKey);
        if (entries.isEmpty()){
            return;
        }
        entries.forEach((skuId, cacheValue) -> {
            CartItemCacheDO cartItem;
            try {
                cartItem = objectMapper.readValue(String.valueOf(cacheValue), CartItemCacheDO.class);
                if (cartItem == null) {
                    throw new BusinessException(Result.SERVER_ERROR, "购物车数据异常");
                }
                cartItem.setSelected(select.getSelected());
                String json = objectMapper.writeValueAsString(cartItem);
                stringRedisTemplate.opsForHash().put(redisKey, String.valueOf(skuId), json);
            } catch (JsonProcessingException e) {
                throw new BusinessException(Result.SERVER_ERROR, "购物车数据格式错误");
            }
        });
    }

    @Override
    public void clearCheckedItems(Long userId, List<Long> skuIds) {
        if (userId == null || skuIds == null || skuIds.isEmpty()) {
            return;
        }

        String redisKey = CartRedisKeys.cart(userId);
        Object[] fields = skuIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray();
        if (fields.length == 0) {
            return;
        }
        stringRedisTemplate.opsForHash().delete(redisKey, fields);
    }

    private List<ProductSkuSnapshotRespDTO> getProductSnapshots(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyList();
        }

        ProductSkuSnapshotRequest request = new ProductSkuSnapshotRequest();
        request.setSkuIds(skuIds);

        Result result = productFeignClient.getSkuSnapshots(request);
        if (result == null || result.getCode() == null || result.getCode() != 0) {
            throw new BusinessException(Result.SERVER_ERROR, "调用商品服务查询 SKU 快照失败");
        }

        return objectMapper.convertValue(result.getData(), new TypeReference<List<ProductSkuSnapshotRespDTO>>() {
        });
    }
}
