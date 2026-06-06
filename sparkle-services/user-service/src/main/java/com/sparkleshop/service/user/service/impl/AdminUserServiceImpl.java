package com.sparkleshop.service.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.PageResponse;
import com.sparkleshop.common.security.enums.UserTypeEnum;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.service.user.constant.UserRedisKeys;
import com.sparkleshop.service.user.dto.admin.AdminUserDetailResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserLevelUpdateRequest;
import com.sparkleshop.service.user.dto.admin.AdminUserPageItemResponse;
import com.sparkleshop.service.user.dto.admin.AdminUserPageRequest;
import com.sparkleshop.service.user.dto.admin.AdminUserStatusUpdateRequest;
import com.sparkleshop.service.user.entity.ShopUserDO;
import com.sparkleshop.service.user.enums.UserStatusEnum;
import com.sparkleshop.service.user.mapper.ShopUserMapper;
import com.sparkleshop.service.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final ShopUserMapper shopUserMapper;
    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<AdminUserPageItemResponse> getUserPage(AdminUserPageRequest request) {
        long pageNo = Math.max(1, request.getPageNo());
        long pageSize = Math.max(1, request.getPageSize());

        Page<ShopUserDO> page = shopUserMapper.selectAdminPage(
                new Page<>(pageNo, pageSize),
                request.getUsername(),
                request.getMobile(),
                request.getStatus()
        );

        PageResponse<AdminUserPageItemResponse> response = new PageResponse<>();
        response.setList(page.getRecords().stream().map(this::toPageItem).toList());
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public AdminUserDetailResponse getUserDetail(Long userId) {
        ShopUserDO user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }
        return toDetail(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, AdminUserStatusUpdateRequest request) {
        Integer status = request.getStatus();
        if (!isValidStatus(status)) {
            throw new BusinessException(40000, "用户状态仅支持 0 或 1");
        }

        ShopUserDO user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }
        if (status.equals(user.getStatus())) {
            return;
        }

        ShopUserDO update = new ShopUserDO();
        update.setId(userId);
        update.setStatus(status);
        shopUserMapper.updateById(update);
        evictUserCache(userId);

        if (UserStatusEnum.DISABLED.getCode() == status) {
            jwtTokenService.invalidateUserTokens(UserTypeEnum.MEMBER.getCode(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserLevel(Long userId, AdminUserLevelUpdateRequest request) {
        ShopUserDO user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }
        if (request.getLevel().equals(user.getLevel())) {
            return;
        }

        ShopUserDO update = new ShopUserDO();
        update.setId(userId);
        update.setLevel(request.getLevel());
        shopUserMapper.updateById(update);
        evictUserCache(userId);
    }

    private AdminUserDetailResponse toDetail(ShopUserDO user) {
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setMobile(user.getMobile());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setLevel(user.getLevel());
        response.setLoginIp(user.getLoginIp());
        response.setLoginTime(user.getLoginTime());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return response;
    }

    private AdminUserPageItemResponse toPageItem(ShopUserDO user) {
        AdminUserPageItemResponse response = new AdminUserPageItemResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setMobile(user.getMobile());
        response.setStatus(user.getStatus());
        response.setLevel(user.getLevel());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private boolean isValidStatus(Integer status) {
        return status != null
                && (UserStatusEnum.DISABLED.getCode() == status || UserStatusEnum.ENABLED.getCode() == status);
    }

    private void evictUserCache(Long userId) {
        stringRedisTemplate.delete(UserRedisKeys.userInfo(userId));
    }
}
