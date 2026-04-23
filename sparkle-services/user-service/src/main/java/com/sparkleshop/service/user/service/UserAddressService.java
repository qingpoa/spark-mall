package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.address.AddressCreateRequest;
import com.sparkleshop.service.user.dto.address.AddressDetailResponse;
import com.sparkleshop.service.user.dto.address.AddressResponse;
import com.sparkleshop.service.user.dto.address.AddressUpdateRequest;

import java.util.List;

public interface UserAddressService {

    List<AddressResponse> listCurrentUserAddresses();

    Long createCurrentUserAddress(AddressCreateRequest request);

    void updateCurrentUserAddress(Long addressId, AddressUpdateRequest request);

    void deleteCurrentUserAddress(Long addressId);

    void setCurrentUserDefaultAddress(Long addressId);

    AddressDetailResponse getAddressDetail(Long userId, Long addressId);
}
