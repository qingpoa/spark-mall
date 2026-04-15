package com.sparkleshop.service.user.service.impl;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.user.dto.address.AddressCreateRequest;
import com.sparkleshop.service.user.dto.address.AddressResponse;
import com.sparkleshop.service.user.dto.address.AddressUpdateRequest;
import com.sparkleshop.service.user.entity.UserAddressDO;
import com.sparkleshop.service.user.mapper.UserAddressMapper;
import com.sparkleshop.service.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.sparkleshop.service.user.constant.UserErrorCodes.ADDRESS_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;
    private final RedissonClient redissonClient;

    @Override
    public List<AddressResponse> listCurrentUserAddresses() {
        List<UserAddressDO> addresses = userAddressMapper.selectByUserId(LoginUserContext.getRequiredUserId());
        List<AddressResponse> responseList = new ArrayList<>(addresses.size());
        for (UserAddressDO address : addresses) {
            responseList.add(toResponse(address));
        }
        return responseList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCurrentUserAddress(AddressCreateRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        if (!isDefault(request.getIsDefault())) {
            return createAddress(userId, request);
        }
        return executeWithDefaultAddressLock(userId, () -> createAddress(userId, request));
    }

    private Long createAddress(Long userId, AddressCreateRequest request) {
        if (isDefault(request.getIsDefault())) {
            userAddressMapper.clearDefaultByUserId(userId);
        }

        UserAddressDO address = new UserAddressDO();
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverMobile(request.getReceiverMobile().trim());
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setIsDefault(isDefault(request.getIsDefault()) ? 1 : 0);
        userAddressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserAddress(Long addressId, AddressUpdateRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        if (!shouldUpdateDefault(userId, addressId, request)) {
            doUpdateCurrentUserAddress(addressId, request, userId);
            return;
        }
        executeWithDefaultAddressLock(userId, () -> {
            doUpdateCurrentUserAddress(addressId, request, userId);
            return null;
        });
    }

    private void doUpdateCurrentUserAddress(Long addressId, AddressUpdateRequest request, Long userId) {
        UserAddressDO existing = getRequiredAddress(addressId, userId);
        Integer isDefault = request.getIsDefault() == null ? existing.getIsDefault() : (isDefault(request.getIsDefault()) ? 1 : 0);
        if (isDefault == 1) {
            userAddressMapper.clearDefaultByUserId(userId);
        }

        UserAddressDO update = new UserAddressDO();
        update.setId(addressId);
        update.setReceiverName(request.getReceiverName().trim());
        update.setReceiverMobile(request.getReceiverMobile().trim());
        update.setProvince(request.getProvince().trim());
        update.setCity(request.getCity().trim());
        update.setDistrict(request.getDistrict().trim());
        update.setDetailAddress(request.getDetailAddress().trim());
        update.setIsDefault(isDefault);
        userAddressMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCurrentUserAddress(Long addressId) {
        Long userId = LoginUserContext.getRequiredUserId();
        getRequiredAddress(addressId, userId);
        userAddressMapper.deleteById(addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCurrentUserDefaultAddress(Long addressId) {
        Long userId = LoginUserContext.getRequiredUserId();
        executeWithDefaultAddressLock(userId, () -> {
            getRequiredAddress(addressId, userId);
            userAddressMapper.clearDefaultByUserId(userId);

            UserAddressDO update = new UserAddressDO();
            update.setId(addressId);
            update.setIsDefault(1);
            userAddressMapper.updateById(update);
            return null;
        });
    }

    private UserAddressDO getRequiredAddress(Long addressId, Long userId) {
        UserAddressDO address = userAddressMapper.selectByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new BusinessException(ADDRESS_NOT_FOUND, "地址不存在");
        }
        return address;
    }

    private AddressResponse toResponse(UserAddressDO address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setReceiverName(address.getReceiverName());
        response.setReceiverMobile(address.getReceiverMobile());
        response.setProvince(address.getProvince());
        response.setCity(address.getCity());
        response.setDistrict(address.getDistrict());
        response.setDetailAddress(address.getDetailAddress());
        response.setIsDefault(address.getIsDefault());
        return response;
    }

    private boolean isDefault(Integer isDefault) {
        return isDefault != null && isDefault == 1;
    }

    private boolean shouldUpdateDefault(Long userId, Long addressId, AddressUpdateRequest request) {
        if (request.getIsDefault() != null) {
            return isDefault(request.getIsDefault());
        }
        UserAddressDO existing = getRequiredAddress(addressId, userId);
        return isDefault(existing.getIsDefault());
    }

    private <T> T executeWithDefaultAddressLock(Long userId, Supplier<T> action) {
        RLock lock = redissonClient.getLock(RedisKeys.USER_ADDRESS_DEFAULT_LOCK + userId);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
