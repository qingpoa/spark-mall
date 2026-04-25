package com.sparkleshop.service.cart.service;

import com.sparkleshop.service.cart.dto.CartAddRequest;
import com.sparkleshop.service.cart.dto.CartSelectAllRequest;
import com.sparkleshop.service.cart.dto.CartSelectRequest;
import com.sparkleshop.service.cart.dto.CartUpdateRequest;
import com.sparkleshop.service.cart.vo.CartAddRespVO;
import com.sparkleshop.service.cart.vo.CartListRespVO;

import java.util.List;

public interface CartService {

    CartListRespVO getCartList();

    CartAddRespVO addCart(CartAddRequest cartAddReqVO);

    void updateCart(CartUpdateRequest cartUpdateReqVO);

    void deleteCartItem(Long skuId);

    void selectCartItem(CartSelectRequest cartSelectReqVO);

    void selectAllCartItem(CartSelectAllRequest select);

    void clearCheckedItems(Long userId, List<Long> skuIds);
}
